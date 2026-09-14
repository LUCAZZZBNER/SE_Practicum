package com.delivery.catalog.item;

import static com.delivery.catalog.ControllerTestSupport.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.delivery.catalog.item.controller.InternalStockController;
import com.delivery.catalog.item.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class InternalStockControllerTest {
  @Test void reserveRequiresInternalToken() throws Exception {
    var service = mock(ItemService.class);
    MockMvc mvc = withApiErrors(new InternalStockController(service, "secret")).build();
    mvc.perform(post("/internal/v1/catalog/stock/reserve").contentType(JSON).content("[]"))
        .andExpect(status().isUnauthorized()).andExpect(unauthenticated());
  }

  @Test void confirmForwardsReservationIds() throws Exception {
    var service = mock(ItemService.class);
    MockMvc mvc = withApiErrors(new InternalStockController(service, "secret")).build();
    mvc.perform(post("/internal/v1/catalog/stock/confirm").header("X-Service-Token", "secret")
        .contentType(JSON).content("[\"r1\",\"r2\"]"))
        .andExpect(status().isNoContent());
    verify(service).confirmReservations(java.util.List.of("r1", "r2"));
  }
}
