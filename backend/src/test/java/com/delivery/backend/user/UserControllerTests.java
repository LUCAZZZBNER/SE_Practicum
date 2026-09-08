package com.delivery.backend.user;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.successfulEnvelope;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.TestFixtures.user;
import static com.delivery.backend.TestFixtures.userSession;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.user.controller.UserController;
import com.delivery.backend.user.service.UserService;

class UserControllerTests {

	private UserService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(UserService.class);
		mvc = withApiErrors(new UserController(service)).build();
	}

	@Test
	void registerReturnsCreatedEnvelopeAndForwardsEveryInput() throws Exception {
		when(service.register(any())).thenReturn(user(1));
		mvc.perform(post("/api/v1/users").contentType(JSON).content("""
				{"account":"alice","password":"secret","passwordConfirm":"secret","nickname":"Alice","phone":"13800000000"}
				"""))
				.andExpect(status().isCreated()).andExpect(successfulDataId(1));

		ArgumentCaptor<UserService.RegisterRequest> request = ArgumentCaptor.forClass(UserService.RegisterRequest.class);
		verify(service).register(request.capture());
		org.assertj.core.api.Assertions.assertThat(request.getValue())
				.isEqualTo(new UserService.RegisterRequest("alice", "secret", "secret", "Alice", "13800000000"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"account\":\"alice\"}",
			"{\"account\":\"alice\",\"password\":\"secret\"}",
			"{\"account\":\"alice\",\"password\":\"secret\",\"passwordConfirm\":\"secret\"}",
			"{\"account\":\" \" ,\"password\":\"secret\",\"passwordConfirm\":\"secret\",\"nickname\":\"Alice\"}",
			"{\"account\":\"alice\",\"password\":\" \",\"passwordConfirm\":\"secret\",\"nickname\":\"Alice\"}",
			"{\"account\":\"alice\",\"password\":\"secret\",\"passwordConfirm\":\" \",\"nickname\":\"Alice\"}",
			"{\"account\":\"alice\",\"password\":\"secret\",\"passwordConfirm\":\"secret\",\"nickname\":\" \",\"phone\":\"138\"}",
			"{\"account\":\"alice\",\"password\":\"secret\",\"passwordConfirm\":\"secret\",\"nickname\":\"Alice\",\"phone\":\" \"}" })
	void registerRejectsEveryMissingOrBlankRequiredField(String body) throws Exception {
		mvc.perform(post("/api/v1/users").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void requestBodiesRejectUnknownFieldsAndMalformedJson() throws Exception {
		mvc.perform(post("/api/v1/users").contentType(JSON).content("""
				{"account":"alice","password":"secret","passwordConfirm":"secret","nickname":"Alice","unknown":true}
				"""))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(post("/api/v1/users").contentType(JSON).content("{"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
	}

	@Test
	void loginReturnsServiceSession() throws Exception {
		when(service.login(any())).thenReturn(userSession(1));
		mvc.perform(post("/api/v1/users/login").contentType(JSON)
				.content("{\"account\":\"alice\",\"password\":\"secret\"}"))
				.andExpect(status().isOk()).andExpect(successfulEnvelope())
				.andExpect(jsonPath("$.data.user.id").value(1));
		verify(service).login(new UserService.LoginRequest("alice", "secret"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"account\":\"alice\"}", "{\"password\":\"secret\"}" })
	void loginRejectsMissingCredentials(String body) throws Exception {
		mvc.perform(post("/api/v1/users/login").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"account\":\" \",\"password\":\"secret\"}",
			"{\"account\":\"alice\",\"password\":\" \"}" })
	void loginRejectsBlankCredentials(String body) throws Exception {
		mvc.perform(post("/api/v1/users/login").contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void currentProfileUsesAuthenticatedPrincipal() throws Exception {
		when(service.getCurrent(7)).thenReturn(user(7));
		mvc.perform(get("/api/v1/users/me").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulDataId(7));
		verify(service).getCurrent(7);
	}

	@Test
	void protectedProfileEndpointsRejectMissingPrincipal() throws Exception {
		mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(patch("/api/v1/users/me").contentType(JSON).content("{\"nickname\":\"New\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
	}

	@Test
	void updateForwardsPresentFieldsAndAllowsNullablePhone() throws Exception {
		when(service.updateCurrent(any(Long.class), any())).thenReturn(user(7));
		mvc.perform(patch("/api/v1/users/me").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"nickname\":\"New\",\"phone\":null}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(7));
		verify(service).updateCurrent(org.mockito.ArgumentMatchers.eq(7L),
				argThat(request -> request.nickname().equals("New") && request.phone() == null));
	}

	@Test
	void updateAcceptsExplicitNullAsAProvidedNullableField() throws Exception {
		when(service.updateCurrent(any(Long.class), any())).thenReturn(user(7));
		mvc.perform(patch("/api/v1/users/me").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"phone\":null}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(7));
		verify(service).updateCurrent(org.mockito.ArgumentMatchers.eq(7L),
				argThat(request -> request.nickname() == null && request.phone() == null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"nickname\":\" \"}", "{\"phone\":\" \"}" })
	void updateRejectsEmptyOrBlankFields(String body) throws Exception {
		mvc.perform(patch("/api/v1/users/me").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}
}
