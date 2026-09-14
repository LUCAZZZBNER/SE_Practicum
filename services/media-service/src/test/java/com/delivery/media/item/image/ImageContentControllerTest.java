package com.delivery.media.item.image;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.delivery.media.item.image.controller.ImageContentController;
import com.delivery.media.item.image.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.delivery.media.common.GlobalExceptionHandler;
import static com.delivery.media.ControllerTestSupport.withApiErrors;

class ImageContentControllerTest {
  @Test void servesBytesWithImmutableCacheHeaders() throws Exception {
    var service = mock(ImageService.class);
    when(service.content(9)).thenReturn(new ImageService.Content(new byte[] {1, 2}, "image/png"));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageContentController(service))
        .setMessageConverters(new ByteArrayHttpMessageConverter())
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    mvc.perform(get("/uploads/products/9")).andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
        .andExpect(content().bytes(new byte[] {1, 2}));
  }
}
