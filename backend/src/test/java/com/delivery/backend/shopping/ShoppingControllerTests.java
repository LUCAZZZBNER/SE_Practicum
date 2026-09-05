package com.delivery.backend.shopping;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.successfulEnvelope;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.TestFixtures.cart;
import static com.delivery.backend.TestFixtures.cartItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.delivery.backend.common.DeleteResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.shopping.controller.ShoppingController;
import com.delivery.backend.shopping.service.ShoppingService;

class ShoppingControllerTests {

	private ShoppingService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(ShoppingService.class);
		mvc = withApiErrors(new ShoppingController(service)).build();
	}

	@Test
	void addForwardsPositiveIdsAndQuantityAndReturnsCreated() throws Exception {
		when(service.add(any(Long.class), any()))
				.thenReturn(new ShoppingService.AddResult(true, cartItem(31)));
		mvc.perform(post("/api/v1/cart-items").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"productId\":30,\"quantity\":1}"))
				.andExpect(status().isCreated()).andExpect(successfulDataId(31));
		verify(service).add(7, new ShoppingService.AddRequest(30, 1));
	}

	@Test
	void addReturnsOkWhenServiceMergesAnExistingCartItem() throws Exception {
		when(service.add(any(Long.class), any()))
				.thenReturn(new ShoppingService.AddResult(false, cartItem(31)));
		mvc.perform(post("/api/v1/cart-items").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"productId\":30,\"quantity\":2}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(31));
		verify(service).add(7, new ShoppingService.AddRequest(30, 2));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"productId\":0,\"quantity\":1}", "{\"productId\":30,\"quantity\":0}",
			"{\"productId\":30,\"quantity\":-1}" })
	void addRejectsMissingZeroAndNegativeInputs(String body) throws Exception {
		mvc.perform(post("/api/v1/cart-items").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void getUpdateAndDeleteForwardUserOwnershipAndReturnOutputs() throws Exception {
		when(service.getCart(7)).thenReturn(cart());
		when(service.changeQuantity(7, 31, 2)).thenReturn(cartItem(31));
		when(service.remove(7, 31)).thenReturn(new DeleteResult(31, true));
		mvc.perform(get("/api/v1/cart-items").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulEnvelope())
				.andExpect(jsonPath("$.data.items[0].id").value(31));
		mvc.perform(patch("/api/v1/cart-items/31").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"quantity\":2}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(31));
		mvc.perform(delete("/api/v1/cart-items/31").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isOk()).andExpect(successfulDataId(31));
		verify(service).getCart(7);
		verify(service).changeQuantity(7, 31, 2);
		verify(service).remove(7, 31);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"quantity\":0}", "{\"quantity\":-1}" })
	void updateRejectsMissingZeroAndNegativeQuantity(String body) throws Exception {
		mvc.perform(patch("/api/v1/cart-items/31").requestAttr("currentPrincipal", userPrincipal(7))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void cartMutationsAcceptMaximumIntegerQuantity() throws Exception {
		when(service.add(org.mockito.ArgumentMatchers.anyLong(), any()))
				.thenReturn(new ShoppingService.AddResult(true, cartItem(31)));
		when(service.changeQuantity(7, 31, Integer.MAX_VALUE)).thenReturn(cartItem(31));
		mvc.perform(post("/api/v1/cart-items").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"productId\":30,\"quantity\":2147483647}"))
				.andExpect(status().isCreated()).andExpect(successfulDataId(31));
		mvc.perform(patch("/api/v1/cart-items/31").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"quantity\":2147483647}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(31));
		verify(service).add(7, new ShoppingService.AddRequest(30, Integer.MAX_VALUE));
		verify(service).changeQuantity(7, 31, Integer.MAX_VALUE);
	}

	@Test
	void cartEndpointsRejectNonPositivePathIds() throws Exception {
		mvc.perform(patch("/api/v1/cart-items/0").requestAttr("currentPrincipal", userPrincipal(7)).contentType(JSON)
				.content("{\"quantity\":1}"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(delete("/api/v1/cart-items/-1").requestAttr("currentPrincipal", userPrincipal(7)))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void allCartEndpointsRejectMissingPrincipal() throws Exception {
		mvc.perform(post("/api/v1/cart-items").contentType(JSON).content("{\"productId\":30,\"quantity\":1}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(get("/api/v1/cart-items"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(patch("/api/v1/cart-items/31").contentType(JSON).content("{\"quantity\":2}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(delete("/api/v1/cart-items/31"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}
}
