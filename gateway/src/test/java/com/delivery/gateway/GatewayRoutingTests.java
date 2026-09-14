package com.delivery.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class GatewayRoutingTests {
  @Test
  void compatibilityFallbackIsUsedWhenNoDomainServiceIsConfigured() throws Exception {
    ApiGatewayController controller =
        new ApiGatewayController(
            org.springframework.web.client.RestClient.builder(),
            "http://backend:8080",
            "",
            "",
            "",
            "",
            "");
    Method method = ApiGatewayController.class.getDeclaredMethod("targetBaseUrl", String.class);
    method.setAccessible(true);
    assertThat(method.invoke(controller, "/api/v1/users/me")).isEqualTo("http://backend:8080");
  }
}
