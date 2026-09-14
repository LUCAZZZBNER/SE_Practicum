package com.delivery.order.checkout;

import static com.delivery.order.ControllerTestSupport.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class CheckoutDecisionControllerTest {
  @Test void returnsDecisionForValidServiceToken() throws Exception {
    var coordinator = mock(CheckoutCoordinator.class);
    when(coordinator.state("c1")).thenReturn("COMMITTED");
    MockMvc mvc = withApiErrors(new CheckoutDecisionController(coordinator, "secret")).build();
    mvc.perform(get("/internal/v1/checkouts/c1").header("X-Service-Token", "secret"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.state").value("COMMITTED"));
  }

  @Test void rejectsWrongToken() throws Exception {
    var coordinator = mock(CheckoutCoordinator.class);
    MockMvc mvc = withApiErrors(new CheckoutDecisionController(coordinator, "secret")).build();
    mvc.perform(get("/internal/v1/checkouts/c1").header("X-Service-Token", "wrong"))
        .andExpect(status().isUnauthorized()).andExpect(unauthenticated());
    verifyNoInteractions(coordinator);
  }
}
