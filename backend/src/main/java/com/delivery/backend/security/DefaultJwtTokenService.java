package com.delivery.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;

/** Minimal HS256 JWT implementation used by the shared authentication infrastructure. */
@Service
public class DefaultJwtTokenService implements JwtTokenService {

	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
	private static final byte[] HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
			.getBytes(StandardCharsets.UTF_8);

	private final byte[] secret;
	private final long expirationSeconds;
	private final Clock clock;
	private final JsonMapper jsonMapper;

	@Autowired
	public DefaultJwtTokenService(@Value("${security.jwt.secret}") String secret,
			@Value("${security.jwt.expiration-seconds:7200}") long expirationSeconds) {
		this(secret, expirationSeconds, Clock.systemUTC());
	}

	public DefaultJwtTokenService(String secret, long expirationSeconds, Clock clock) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
		}
		if (expirationSeconds < 1) {
			throw new IllegalArgumentException("JWT expiration must be positive");
		}
		this.secret = secret.getBytes(StandardCharsets.UTF_8).clone();
		this.expirationSeconds = expirationSeconds;
		this.clock = clock;
		this.jsonMapper = JsonMapper.builder().build();
	}

	@Override
	public TokenSession issue(long principalId, Role role) {
		CurrentPrincipal principal = new CurrentPrincipal(principalId, role);
		long issuedAt = Instant.now(clock).getEpochSecond();
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", Long.toString(principal.id()));
		claims.put("role", principal.role().name());
		claims.put("iat", issuedAt);
		claims.put("exp", issuedAt + expirationSeconds);
		try {
			String header = URL_ENCODER.encodeToString(HEADER);
			String payload = URL_ENCODER.encodeToString(jsonMapper.writeValueAsBytes(claims));
			String content = header + "." + payload;
			String signature = URL_ENCODER.encodeToString(sign(content));
			return new TokenSession(content + "." + signature, "Bearer", expirationSeconds);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to issue access token", exception);
		}
	}

	@Override
	public CurrentPrincipal parse(String token) {
		try {
			String[] parts = token == null ? new String[0] : token.split("\\.", -1);
			if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
				throw unauthenticated();
			}
			Map<String, Object> header = jsonMapper.readValue(URL_DECODER.decode(parts[0]),
					new TypeReference<Map<String, Object>>() { });
			if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
				throw unauthenticated();
			}
			byte[] expected = sign(parts[0] + "." + parts[1]);
			byte[] supplied = URL_DECODER.decode(parts[2]);
			if (!MessageDigest.isEqual(expected, supplied)) {
				throw unauthenticated();
			}
			Map<String, Object> claims = jsonMapper.readValue(URL_DECODER.decode(parts[1]),
					new TypeReference<Map<String, Object>>() { });
			long principalId = Long.parseLong(String.valueOf(claims.get("sub")));
			Role role = Role.valueOf(String.valueOf(claims.get("role")));
			long expiresAt = ((Number) claims.get("exp")).longValue();
			if (Instant.now(clock).getEpochSecond() >= expiresAt) {
				throw unauthenticated();
			}
			return new CurrentPrincipal(principalId, role);
		} catch (BusinessException exception) {
			throw exception;
		} catch (Exception exception) {
			throw unauthenticated();
		}
	}

	private byte[] sign(String content) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "HmacSHA256"));
		return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(ApiError.UNAUTHENTICATED);
	}
}
