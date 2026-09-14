package com.delivery.cart.shopping;

import static com.delivery.cart.ControllerTestSupport.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.delivery.cart.shopping.controller.InternalCartController;
import com.delivery.cart.shopping.service.ShoppingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class InternalCartControllerTest {
  @Test void rejectsMissingServiceToken() throws Exception {
    var service = mock(ShoppingService.class);
    MockMvc mvc = withApiErrors(new InternalCartController(service, "secret")).build();
    mvc.perform(post("/internal/v1/cart/prepare-checkout").contentType(JSON)
        .content("{\"userId\":7,\"cartItemIds\":[3],\"checkoutId\":\"c1\"}"))
        .andExpect(status().isUnauthorized()).andExpect(unauthenticated());
    verifyNoInteractions(service);
  }

  @Test void loadsItemsWithValidToken() throws Exception {
    var service = mock(ShoppingService.class);
    when(service.loadForCheckout(7, java.util.List.of(3L))).thenReturn(java.util.List.of());
    MockMvc mvc = withApiErrors(new InternalCartController(service, "secret")).build();
    mvc.perform(post("/internal/v1/cart/load-for-checkout").header("X-Service-Token", "secret")
        .contentType(JSON).content("{\"userId\":7,\"cartItemIds\":[3]}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    verify(service).loadForCheckout(7, java.util.List.of(3L));
  }
}
