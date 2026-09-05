package com.delivery.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;

class DefaultJwtTokenServiceTests {

	private static final String SECRET = "01234567890123456789012345678901";
	private static final Instant NOW = Instant.parse("2026-09-05T00:00:00Z");

	@Test
	void issuedTokenRoundTripsEveryPrincipalRole() {
		DefaultJwtTokenService service = serviceAt(NOW);
		JwtTokenService.TokenSession user = service.issue(7, Role.USER);
		JwtTokenService.TokenSession merchant = service.issue(2, Role.MERCHANT);

		assertThat(user.tokenType()).isEqualTo("Bearer");
		assertThat(user.expiresIn()).isEqualTo(7200);
		assertThat(service.parse(user.accessToken())).isEqualTo(new CurrentPrincipal(7, Role.USER));
		assertThat(service.parse(merchant.accessToken())).isEqualTo(new CurrentPrincipal(2, Role.MERCHANT));
	}

	@Test
	void tokenExpiresAtTheExactExpirationBoundary() {
		String token = serviceAt(NOW).issue(7, Role.USER).accessToken();

		assertUnauthenticated(() -> serviceAt(NOW.plusSeconds(7200)).parse(token));
	}

	@Test
	void malformedNullAndTamperedTokensAreRejected() {
		DefaultJwtTokenService service = serviceAt(NOW);
		String token = service.issue(7, Role.USER).accessToken();
		String header = token.substring(0, token.indexOf('.'));

		assertUnauthenticated(() -> service.parse(null));
		assertUnauthenticated(() -> service.parse("not-a-jwt"));
		assertUnauthenticated(() -> service.parse("e30.payload.signature"));
		assertUnauthenticated(() -> service.parse(header + ".%.x"));
		assertUnauthenticated(() -> service.parse(token.substring(0, token.length() - 1) + "x"));
	}

	@Test
	void invalidConfigurationAndPrincipalInputsAreRejected() {
		assertThat(new DefaultJwtTokenService(SECRET, 7200).issue(1, Role.USER).expiresIn()).isEqualTo(7200);
		assertThatThrownBy(() -> new DefaultJwtTokenService("short", 7200, Clock.systemUTC()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DefaultJwtTokenService(SECRET, 0, Clock.systemUTC()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> serviceAt(NOW).issue(0, Role.USER))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static DefaultJwtTokenService serviceAt(Instant instant) {
		return new DefaultJwtTokenService(SECRET, 7200, Clock.fixed(instant, ZoneOffset.UTC));
	}

	private static void assertUnauthenticated(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.error()).isEqualTo(ApiError.UNAUTHENTICATED));
	}
}
