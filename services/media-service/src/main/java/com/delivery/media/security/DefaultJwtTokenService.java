package com.delivery.media.security;

import com.delivery.media.common.ApiError;
import com.delivery.media.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Spring Security JWT implementation shared by the application security infrastructure. */
@org.springframework.stereotype.Service
public class DefaultJwtTokenService implements JwtTokenService {
  private final long expirationSeconds;
  private final Clock clock;
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;

  @Autowired
  public DefaultJwtTokenService(@Value("${security.jwt.secret}") String secret,
      @Value("${security.jwt.expiration-seconds:7200}") long expirationSeconds) {
    this(secret, expirationSeconds, Clock.systemUTC());
  }

  public DefaultJwtTokenService(String secret, long expirationSeconds, Clock clock) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
    if (expirationSeconds < 1) throw new IllegalArgumentException("JWT expiration must be positive");
    this.expirationSeconds = expirationSeconds;
    this.clock = clock;
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    JwtTimestampValidator validator = new JwtTimestampValidator();
    validator.setClock(clock);
    decoder.setJwtValidator(validator);
    this.decoder = decoder;
  }

  @Override
  public TokenSession issue(long principalId, Role role) {
    CurrentPrincipal principal = new CurrentPrincipal(principalId, role);
    Instant issuedAt = Instant.now(clock);
    JwtClaimsSet claims = JwtClaimsSet.builder().subject(Long.toString(principal.id()))
        .claim("role", principal.role().name()).issuedAt(issuedAt)
        .expiresAt(issuedAt.plusSeconds(expirationSeconds)).build();
    String token = encoder.encode(JwtEncoderParameters.from(
        org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    return new TokenSession(token, "Bearer", expirationSeconds);
  }

  @Override
  public CurrentPrincipal parse(String token) {
    try {
      Jwt jwt = decoder.decode(token);
      if (jwt.getExpiresAt() == null || !Instant.now(clock).isBefore(jwt.getExpiresAt())) throw new BusinessException(ApiError.UNAUTHENTICATED);
      return new CurrentPrincipal(Long.parseLong(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString("role")));
    } catch (Exception ex) { throw new BusinessException(ApiError.UNAUTHENTICATED); }
  }
}
