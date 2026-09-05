package com.delivery.backend.merchant;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulEnvelope;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.TestFixtures.merchant;
import static com.delivery.backend.TestFixtures.merchantSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.merchant.controller.MerchantController;
import com.delivery.backend.merchant.service.MerchantService;

class MerchantControllerTests {

	private MerchantService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(MerchantService.class);
		mvc = withApiErrors(new MerchantController(service)).build();
	}

	@Test
	void registerReturnsCreatedAndForwardsOptionalPasswordConfirmation() throws Exception {
		when(service.register(any())).thenReturn(merchant(2));
		mvc.perform(post("/api/v1/merchants").contentType(JSON).content("""
				{"account":"shopper","password":"secret","name":"Store","phone":"13900000000"}
				"""))
				.andExpect(status().isCreated()).andExpect(successfulDataId(2));
		verify(service).register(new MerchantService.RegisterRequest("shopper", "secret", null, "Store", "13900000000"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"account\":\"shopper\"}",
			"{\"account\":\"shopper\",\"password\":\"secret\"}",
			"{\"account\":\"shopper\",\"password\":\"secret\",\"name\":\"Store\"}",
			"{\"account\":\" \",\"password\":\"secret\",\"name\":\"Store\",\"phone\":\"139\"}",
			"{\"account\":\"shopper\",\"password\":\" \",\"name\":\"Store\",\"phone\":\"139\"}",
			"{\"account\":\"shopper\",\"password\":\"secret\",\"name\":\" \",\"phone\":\"13900000000\"}",
			"{\"account\":\"shopper\",\"password\":\"secret\",\"name\":\"Store\",\"phone\":\" \"}" })
	void registerRejectsEachMissingOrBlankRequiredField(String body) throws Exception {
		mvc.perform(post("/api/v1/merchants").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void loginReturnsServiceSessionAndForwardsCredentials() throws Exception {
		when(service.login(any())).thenReturn(merchantSession(2));
		mvc.perform(post("/api/v1/merchants/login").contentType(JSON)
				.content("{\"account\":\"shopper\",\"password\":\"secret\"}"))
				.andExpect(status().isOk()).andExpect(successfulEnvelope())
				.andExpect(jsonPath("$.data.merchant.id").value(2));
		verify(service).login(new MerchantService.LoginRequest("shopper", "secret"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"account\":\"shopper\"}", "{\"password\":\"secret\"}" })
	void loginRejectsEveryMissingCredential(String body) throws Exception {
		mvc.perform(post("/api/v1/merchants/login").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"account\":\" \",\"password\":\"secret\"}",
			"{\"account\":\"shopper\",\"password\":\" \"}" })
	void loginRejectsBlankCredentials(String body) throws Exception {
		mvc.perform(post("/api/v1/merchants/login").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void profileReadAndUpdateForwardMerchantPrincipalAndFields() throws Exception {
		when(service.getCurrent(2)).thenReturn(merchant(2));
		when(service.updateCurrent(any(Long.class), any())).thenReturn(merchant(2));
		mvc.perform(get("/api/v1/merchants/me").requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isOk()).andExpect(successfulDataId(2));
		mvc.perform(patch("/api/v1/merchants/me").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"New Store\",\"phone\":\"13900000001\"}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(2));
		verify(service).getCurrent(2);
		verify(service).updateCurrent(org.mockito.ArgumentMatchers.eq(2L), argThat(request ->
				request.name().equals("New Store") && request.phone().equals("13900000001")));
	}

	@Test
	void protectedProfileEndpointsRejectMissingPrincipal() throws Exception {
		mvc.perform(get("/api/v1/merchants/me")).andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(patch("/api/v1/merchants/me").contentType(JSON).content("{\"name\":\"New Store\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\":\" \"}", "{\"phone\":\" \"}" })
	void updateRejectsEmptyOrBlankFields(String body) throws Exception {
		mvc.perform(patch("/api/v1/merchants/me").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}
}
