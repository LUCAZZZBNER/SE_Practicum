package com.delivery.backend.image;

import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.image.controller.ImageController;
import com.delivery.backend.image.service.ImageService;

class ImageControllerTests {

	private ImageService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(ImageService.class);
		mvc = withApiErrors(new ImageController(service)).build();
	}

	@Test
	void uploadForwardsMerchantAndMultipartFile() throws Exception {
		when(service.upload(any(Long.class), any())).thenReturn(
				new ImageService.ImageView(301, "/uploads/products/301.webp", "image/webp", 4,
						Instant.parse("2026-09-11T00:00:00Z")));
		MockMultipartFile file = new MockMultipartFile("file", "product.webp", "image/webp",
				new byte[] { 1, 2, 3, 4 });
		mvc.perform(multipart("/api/v1/files/images").file(file)
				.requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isCreated()).andExpect(successfulDataId(301));
		verify(service).upload(2, file);
	}

	@Test
	void uploadRejectsMissingAuthentication() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "product.jpg", "image/jpeg", new byte[] { 1 });
		mvc.perform(multipart("/api/v1/files/images").file(file))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
	}
}
