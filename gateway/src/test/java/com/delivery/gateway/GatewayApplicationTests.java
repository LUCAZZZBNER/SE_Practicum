package com.delivery.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {
  @LocalServerPort int port;

  @Test
  void startsAsAnIndependentDeployableApplication() {
    assertThat(port).isPositive();
  }
}
