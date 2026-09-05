package com.delivery.backend.order;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.successfulPage;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.TestFixtures.order;
import static com.delivery.backend.TestFixtures.orderPage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.order.controller.OrderController;
import com.delivery.backend.order.service.OrderService;

class OrderControllerTests {

	private OrderService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(OrderService.class);
		mvc = withApiErrors(new OrderController(service)).build();
	}

	@Test
	void createRequiresIdempotencyKeyAndForwardsEveryItemVersion() throws Exception {
		when(service.create(any(Long.class), any(String.class), any())).thenReturn(order(40));
		mvc.perform(post("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.header("X-Idempotency-Key", "key-1")
				.contentType(JSON).content("""
				{"items":[{"cartItemId":31,"productVersion":3},{"cartItemId":32,"productVersion":7}]}
				"""))
				.andExpect(status().isCreated()).andExpect(successfulDataId(40));
		verify(service).create(7, "key-1", new OrderService.CreateRequest(
				List.of(new OrderService.ItemRequest(31, 3), new OrderService.ItemRequest(32, 7))));
	}

	@Test
	void createRejectsMissingPrincipalHeaderBlankHeaderOrItems() throws Exception {
		String valid = "{\"items\":[{\"cartItemId\":31,\"productVersion\":3}]}";
		mvc.perform(post("/api/v1/orders").header("X-Idempotency-Key", "key-1").contentType(JSON).content(valid))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(post("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content(valid))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(post("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.header("X-Idempotency-Key", " ")
				.contentType(JSON).content(valid))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(post("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.header("X-Idempotency-Key", "key-1")
				.contentType(JSON).content("{\"items\":[]}"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"items\":[null]}",
			"{\"items\":[{\"cartItemId\":0,\"productVersion\":3}]}",
			"{\"items\":[{\"cartItemId\":31,\"productVersion\":0}]}",
			"{\"items\":[{\"cartItemId\":-1,\"productVersion\":3}]}",
			"{\"items\":[{\"cartItemId\":31,\"productVersion\":-1}]}" })
	void createRejectsZeroAndNegativeItemInputs(String body) throws Exception {
		mvc.perform(post("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.header("X-Idempotency-Key", "key-1")
				.contentType(JSON).content(body)).andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void userListForwardsAbsentOptionalParameters() throws Exception {
		when(service.listMine(any(Long.class), any())).thenReturn(orderPage());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listMine(7, new OrderService.ListQuery(null, null, null, null, null));
	}

	@Test
	void userListForwardsEveryFilterPagingAndSortInput() throws Exception {
		when(service.listMine(any(Long.class), any())).thenReturn(orderPage());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7))
				.queryParam("status", "PENDING_PAYMENT")
				.queryParam("page", "1").queryParam("pageSize", "100").queryParam("sortBy", "total")
				.queryParam("sortOrder", "desc"))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listMine(7, new OrderService.ListQuery("PENDING_PAYMENT", 1, 100, "total", "desc"));
	}

	@Test
	void userGetAndCancelForwardOwnershipAndOrderId() throws Exception {
		when(service.getMine(7, 40)).thenReturn(order(40));
		when(service.cancel(7, 40)).thenReturn(order(40));
		mvc.perform(get("/api/v1/orders/40").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulDataId(40));
		mvc.perform(post("/api/v1/orders/40/cancel").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulDataId(40));
		verify(service).getMine(7, 40);
		verify(service).cancel(7, 40);
	}

	@Test
	void merchantListForwardsAbsentAndFullySpecifiedQueries() throws Exception {
		when(service.listMerchantOrders(any(Long.class), any())).thenReturn(orderPage());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listMerchantOrders(2, new OrderService.MerchantListQuery(null, null, null, null, null, null));

		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2))
				.queryParam("shopId", "10")
				.queryParam("status", "PAID").queryParam("page", "1").queryParam("pageSize", "100")
				.queryParam("sortBy", "createdAt").queryParam("sortOrder", "asc"))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listMerchantOrders(2, new OrderService.MerchantListQuery(10L, "PAID", 1, 100, "createdAt", "asc"));
	}

	@Test
	void merchantDetailForwardsMerchantOwnershipAndOrderId() throws Exception {
		when(service.getMerchantOrder(2, 40)).thenReturn(order(40));
		mvc.perform(get("/api/v1/merchant/orders/40").requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isOk()).andExpect(successfulDataId(40));
		verify(service).getMerchantOrder(2, 40);
	}

	@Test
	void orderQueriesRejectMalformedNumbersAndProtectedEndpointsRejectMissingPrincipal() throws Exception {
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("page", "text"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2))
				.queryParam("shopId", "text"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/orders/40"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(get("/api/v1/merchant/orders/40"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}

	@Test
	void userOrderListRejectsPagingStatusAndSortingOutsideDocumentedDomains() throws Exception {
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("page", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("pageSize", "101"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("status", "UNKNOWN"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("sortBy", "id"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/orders").requestAttr("currentPrincipal", userPrincipal(7)).queryParam("sortOrder", "DESC"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void merchantOrderListRejectsInvalidShopIdPagingStatusAndSorting() throws Exception {
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("shopId", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("page", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("pageSize", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("status", "UNKNOWN"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("sortBy", "id"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders").requestAttr("currentPrincipal", merchantPrincipal(2)).queryParam("sortOrder", "DESC"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void detailAndCancelRejectNonPositiveOrderIds() throws Exception {
		mvc.perform(get("/api/v1/orders/0").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(post("/api/v1/orders/-1/cancel").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/merchant/orders/0").requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void everyProtectedOrderEndpointRejectsMissingPrincipal() throws Exception {
		mvc.perform(get("/api/v1/orders"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(post("/api/v1/orders/40/cancel"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(get("/api/v1/merchant/orders"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}
}
