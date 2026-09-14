package com.delivery.identity.merchant;

import static com.delivery.identity.ControllerTestSupport.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.delivery.identity.merchant.controller.InternalMerchantController;
import com.delivery.identity.merchant.service.MerchantService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class InternalMerchantControllerTest {
  @Test void returnsActiveMerchantSnapshot() throws Exception {
    var service = mock(MerchantService.class);
    when(service.requireActive(7)).thenReturn(new MerchantService.MerchantSnapshot(7, "ACTIVE"));
    MockMvc mvc = withApiErrors(new InternalMerchantController(service)).build();
    mvc.perform(get("/internal/v1/merchants/7")).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}
