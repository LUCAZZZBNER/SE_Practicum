package com.delivery.backend.address;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.successfulEnvelope;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.address.controller.UserAddressController;
import com.delivery.backend.address.service.UserAddressService;

class UserAddressControllerTests {

	private UserAddressService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(UserAddressService.class);
		mvc = withApiErrors(new UserAddressController(service)).build();
	}

	@Test
	void createListUpdateAndDeleteForwardUserOwnership() throws Exception {
		when(service.create(any(Long.class), any())).thenReturn(address(51));
		when(service.list(7)).thenReturn(List.of(address(51)));
		when(service.update(eq(7L), eq(51L), any())).thenReturn(address(51));
		when(service.remove(7, 51)).thenReturn(new com.delivery.backend.common.DeleteResult(51, true));

		mvc.perform(post("/api/v1/user-addresses").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content("""
				{"recipient":"张三","phone":"13800000000","region":"杭州","detail":"学院路 2 号","isDefault":true}
				"""))
				.andExpect(status().isCreated()).andExpect(successfulDataId(51));
		mvc.perform(get("/api/v1/user-addresses").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulEnvelope());
		mvc.perform(patch("/api/v1/user-addresses/51").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content("{\"detail\":\"新地址\"}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(51));
		mvc.perform(delete("/api/v1/user-addresses/51").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulDataId(51));

		verify(service).create(eq(7L), any());
		verify(service).list(7);
		verify(service).update(eq(7L), eq(51L), any());
		verify(service).remove(7, 51);
	}

	@Test
	void rejectsInvalidAddressAndMissingAuthentication() throws Exception {
		mvc.perform(post("/api/v1/user-addresses").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content("{\"recipient\":\"\",\"phone\":\"bad\"}"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/user-addresses"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}

	private static UserAddressService.AddressView address(long id) {
		return new UserAddressService.AddressView(id, 7, "张三", "13800000000", "杭州", "学院路",
				true, Instant.parse("2026-09-11T00:00:00Z"), Instant.parse("2026-09-11T00:00:00Z"));
	}
}
