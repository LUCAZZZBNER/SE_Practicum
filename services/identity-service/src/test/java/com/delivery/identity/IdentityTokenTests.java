package com.delivery.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.delivery.identity.security.CurrentPrincipal;
import com.delivery.identity.security.DefaultJwtTokenService;
import com.delivery.identity.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IdentityTokenTests {
  @Test
  void issuedTokensRoundTripThePrincipalAndRole() {
    DefaultJwtTokenService service =
        new DefaultJwtTokenService(
            "01234567890123456789012345678901", 3600,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    String token = service.issue(42L, Role.USER).accessToken();
    assertThat(service.parse(token)).isEqualTo(new CurrentPrincipal(42L, Role.USER));
  }
}
