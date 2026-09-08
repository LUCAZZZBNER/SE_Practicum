package com.delivery.backend.restaurant;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.successfulPage;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.TestFixtures.shop;
import static com.delivery.backend.TestFixtures.shopPage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.restaurant.controller.RestaurantController;
import com.delivery.backend.restaurant.service.RestaurantService;

class RestaurantControllerTests {

	private RestaurantService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(RestaurantService.class);
		mvc = withApiErrors(new RestaurantController(service)).build();
	}

	@Test
	void createReturnsCreatedAndForwardsMerchantAndNullableDescription() throws Exception {
		when(service.create(any(Long.class), any())).thenReturn(shop(10));
		mvc.perform(post("/api/v1/shops").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"Shop\",\"description\":null}"))
				.andExpect(status().isCreated()).andExpect(successfulDataId(10));
		verify(service).create(2, new RestaurantService.CreateRequest("Shop", null));
	}

	@Test
	void createRejectsBlankNameAndMissingPrincipal() throws Exception {
		mvc.perform(post("/api/v1/shops").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\" \"}"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(post("/api/v1/shops").contentType(JSON).content("{\"name\":\"Shop\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
	}

	@Test
	void publicListForwardsAbsentOptionalParametersAsNull() throws Exception {
		when(service.list(any())).thenReturn(shopPage());
		mvc.perform(get("/api/v1/shops")).andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).list(new RestaurantService.ListQuery(null, null, null, null, null, null, null, null));
	}

	@Test
	void merchantListForwardsEveryFilterSortAndIdentityInput() throws Exception {
		when(service.list(any())).thenReturn(shopPage());
		mvc.perform(get("/api/v1/shops").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("page", "1")
				.queryParam("pageSize", "100").queryParam("keyword", "rice").queryParam("status", "OPEN")
				.queryParam("mine", "true").queryParam("sortBy", "name").queryParam("sortOrder", "asc"))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).list(new RestaurantService.ListQuery(1, 100, "rice", "OPEN", true, "name", "asc", 2L));

		mvc.perform(get("/api/v1/shops").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("mine", "true"))
				.andExpect(status().isOk());
		verify(service).list(new RestaurantService.ListQuery(null, null, null, null, true, null, null, null));
	}

	@Test
	void listRejectsMalformedTypedQueryParameters() throws Exception {
		mvc.perform(get("/api/v1/shops").queryParam("page", "text"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops").queryParam("mine", "text"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
	}

	@Test
	void listRejectsPagingEnumAndSortingOutsideDocumentedDomains() throws Exception {
		mvc.perform(get("/api/v1/shops").queryParam("page", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops").queryParam("pageSize", "101"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops").queryParam("status", "INVALID"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops").queryParam("sortBy", "id"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops").queryParam("sortOrder", "ASC"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void getAndUpdateForwardIdsStateAndNullableDescription() throws Exception {
		when(service.get(10)).thenReturn(shop(10));
		when(service.update(any(Long.class), any(Long.class), any())).thenReturn(shop(10));
		mvc.perform(get("/api/v1/shops/10")).andExpect(status().isOk()).andExpect(successfulDataId(10));
		mvc.perform(patch("/api/v1/shops/10").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"Renamed\",\"description\":null,\"status\":\"CLOSED\"}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(10));
		verify(service).get(10);
		verify(service).update(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(10L),
				argThat(request -> request.name().equals("Renamed") && request.description() == null
						&& request.status().equals("CLOSED")));
	}

	@Test
	void updateAcceptsExplicitNullAsAProvidedNullableDescription() throws Exception {
		when(service.update(any(Long.class), any(Long.class), any())).thenReturn(shop(10));
		mvc.perform(patch("/api/v1/shops/10").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"description\":null}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(10));
		verify(service).update(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(10L),
				argThat(request -> request.name() == null && request.description() == null && request.status() == null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\":\" \"}", "{\"status\":\"INVALID\"}" })
	void updateRejectsEmptyBlankAndUnknownStatus(String body) throws Exception {
		mvc.perform(patch("/api/v1/shops/10").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void shopEndpointsRejectNonPositivePathIdsAndUpdateRequiresPrincipal() throws Exception {
		mvc.perform(get("/api/v1/shops/0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/-1"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(patch("/api/v1/shops/10").contentType(JSON).content("{\"name\":\"New\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}
}
