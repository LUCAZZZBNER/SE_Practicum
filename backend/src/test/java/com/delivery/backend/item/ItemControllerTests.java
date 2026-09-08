package com.delivery.backend.item;

import static com.delivery.backend.ControllerTestSupport.JSON;
import static com.delivery.backend.ControllerTestSupport.merchantPrincipal;
import static com.delivery.backend.ControllerTestSupport.successfulDataId;
import static com.delivery.backend.ControllerTestSupport.successfulEnvelope;
import static com.delivery.backend.ControllerTestSupport.successfulPage;
import static com.delivery.backend.ControllerTestSupport.unauthenticated;
import static com.delivery.backend.ControllerTestSupport.userPrincipal;
import static com.delivery.backend.ControllerTestSupport.validationError;
import static com.delivery.backend.ControllerTestSupport.withApiErrors;
import static com.delivery.backend.TestFixtures.category;
import static com.delivery.backend.TestFixtures.product;
import static com.delivery.backend.TestFixtures.productPage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
import java.util.List;

import com.delivery.backend.common.DeleteResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.backend.item.controller.ItemController;
import com.delivery.backend.item.service.ItemService;

class ItemControllerTests {

	private ItemService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = org.mockito.Mockito.mock(ItemService.class);
		mvc = withApiErrors(new ItemController(service)).build();
	}

	@Test
	void categoryEndpointsForwardEveryRequestAndReturnServiceOutput() throws Exception {
		when(service.createCategory(any(Long.class), any(Long.class), any())).thenReturn(category(21));
		when(service.listCategories(10)).thenReturn(List.of(category(10)));
		when(service.updateCategory(any(Long.class), any(Long.class), any())).thenReturn(category(21));
		when(service.deleteCategory(2, 21)).thenReturn(new DeleteResult(21, true));

		mvc.perform(post("/api/v1/shops/10/categories").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"Meals\",\"sortOrder\":0}"))
				.andExpect(status().isCreated()).andExpect(successfulDataId(21));
		mvc.perform(get("/api/v1/shops/10/categories"))
				.andExpect(status().isOk()).andExpect(successfulEnvelope())
				.andExpect(jsonPath("$.data[0].id").value(10));
		mvc.perform(patch("/api/v1/categories/21").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"Main Meals\",\"sortOrder\":1}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(21));
		mvc.perform(delete("/api/v1/categories/21").requestAttr("currentPrincipal", merchantPrincipal(2)))
				.andExpect(status().isOk()).andExpect(successfulDataId(21));

		verify(service).createCategory(2, 10, new ItemService.CreateCategoryRequest("Meals", 0));
		verify(service).listCategories(10);
		verify(service).updateCategory(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(21L),
				argThat(request -> request.name().equals("Main Meals") && request.sortOrder() == 1));
		verify(service).deleteCategory(2, 21);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\":\" \"}", "{\"name\":\"Meals\",\"sortOrder\":-1}" })
	void createCategoryRejectsMissingBlankAndNegativeValues(String body) throws Exception {
		mvc.perform(post("/api/v1/shops/10/categories").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void createProductForwardsDecimalAndZeroStockBoundary() throws Exception {
		when(service.createProduct(any(Long.class), any())).thenReturn(product(30));
		mvc.perform(post("/api/v1/products").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON).content("""
				{"shopId":10,"categoryId":21,"name":"Rice","description":null,"price":0.01,"stock":0}
				"""))
				.andExpect(status().isCreated()).andExpect(successfulDataId(30));
		verify(service).createProduct(2,
				new ItemService.CreateProductRequest(10, 21, "Rice", null, new BigDecimal("0.01"), 0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"{\"shopId\":0,\"categoryId\":21,\"name\":\"Rice\",\"price\":1,\"stock\":0}",
			"{\"shopId\":10,\"categoryId\":0,\"name\":\"Rice\",\"price\":1,\"stock\":0}",
			"{\"shopId\":10,\"categoryId\":21,\"name\":\"\",\"price\":1,\"stock\":0}",
			"{\"shopId\":10,\"categoryId\":21,\"name\":\"Rice\",\"price\":0,\"stock\":0}",
			"{\"shopId\":10,\"categoryId\":21,\"name\":\"Rice\",\"price\":1.001,\"stock\":0}",
			"{\"shopId\":10,\"categoryId\":21,\"name\":\"Rice\",\"price\":1,\"stock\":-1}" })
	void createProductRejectsMissingInvalidIdsNamePriceAndStock(String body) throws Exception {
		mvc.perform(post("/api/v1/products").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void productListForwardsAbsentAndFullySpecifiedQueries() throws Exception {
		when(service.listProducts(any(Long.class), any())).thenReturn(productPage());
		mvc.perform(get("/api/v1/shops/10/products"))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listProducts(10, new ItemService.ProductQuery(null, null, null, null, null, null, null, null));

		mvc.perform(get("/api/v1/shops/10/products").requestAttr("currentPrincipal", merchantPrincipal(2))
				.queryParam("categoryId", "21")
				.queryParam("keyword", "rice")
				.queryParam("page", "1").queryParam("pageSize", "100").queryParam("sortBy", "price")
				.queryParam("sortOrder", "desc").queryParam("includeOffSale", "true"))
				.andExpect(status().isOk()).andExpect(successfulPage(1, 10, 1));
		verify(service).listProducts(10, new ItemService.ProductQuery(21L, "rice", 1, 100, "price", "desc", true, 2L));

		mvc.perform(get("/api/v1/shops/10/products").requestAttr("currentPrincipal", userPrincipal(7))
				.queryParam("includeOffSale", "true"))
				.andExpect(status().isOk());
		verify(service).listProducts(10,
				new ItemService.ProductQuery(null, null, null, null, null, null, true, null));
	}

	@Test
	void productListRejectsIdsPagingSortAndBooleanOutsideDocumentedDomains() throws Exception {
		mvc.perform(get("/api/v1/shops/0/products"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("categoryId", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("page", "0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("pageSize", "101"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("sortBy", "stock"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("sortOrder", "ASC"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/shops/10/products").queryParam("includeOffSale", "text"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void productGetDefaultsOffSaleToFalseAndAcceptsTrue() throws Exception {
		when(service.getProduct(any(Long.class), org.mockito.ArgumentMatchers.anyBoolean(),
				org.mockito.ArgumentMatchers.nullable(Long.class))).thenReturn(product(30));
		mvc.perform(get("/api/v1/products/30")).andExpect(status().isOk()).andExpect(successfulDataId(30));
		mvc.perform(get("/api/v1/products/30").requestAttr("currentPrincipal", merchantPrincipal(2))
				.queryParam("includeOffSale", "true"))
				.andExpect(status().isOk()).andExpect(successfulDataId(30));
		mvc.perform(get("/api/v1/products/30").requestAttr("currentPrincipal", userPrincipal(7))
				.queryParam("includeOffSale", "true"))
				.andExpect(status().isOk()).andExpect(successfulDataId(30));
		verify(service).getProduct(30, false, null);
		verify(service).getProduct(30, true, 2L);
		verify(service).getProduct(30, true, null);
	}

	@Test
	void updateProductForwardsAllOptionalFieldsIncludingVersion() throws Exception {
		when(service.updateProduct(any(Long.class), any(Long.class), any())).thenReturn(product(30));
		mvc.perform(patch("/api/v1/products/30").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON).content("""
				{"categoryId":22,"name":"Noodles","description":null,"price":12.50,"stock":5,"status":"ON_SALE","version":3}
				"""))
				.andExpect(status().isOk()).andExpect(successfulDataId(30));
		verify(service).updateProduct(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(30L),
				argThat(request -> request.categoryId() == 22L && request.name().equals("Noodles")
						&& request.description() == null && request.price().equals(new BigDecimal("12.50"))
						&& request.stock() == 5 && request.status().equals("ON_SALE") && request.version() == 3L));
	}

	@Test
	void updateProductAcceptsExplicitNullAsAProvidedNullableDescription() throws Exception {
		when(service.updateProduct(any(Long.class), any(Long.class), any())).thenReturn(product(30));
		mvc.perform(patch("/api/v1/products/30").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"description\":null}"))
				.andExpect(status().isOk()).andExpect(successfulDataId(30));
		verify(service).updateProduct(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(30L),
				argThat(request -> request.categoryId() == null && request.name() == null
						&& request.description() == null && request.price() == null && request.stock() == null
						&& request.status() == null && request.version() == null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"categoryId\":0}", "{\"name\":\" \"}", "{\"price\":0}",
			"{\"price\":1.001}", "{\"stock\":-1}", "{\"status\":\"INVALID\"}", "{\"version\":0}" })
	void updateProductRejectsEmptyAndEveryInvalidFieldDomain(String body) throws Exception {
		mvc.perform(patch("/api/v1/products/30").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\":\" \"}", "{\"sortOrder\":-1}" })
	void updateCategoryRejectsEmptyBlankAndNegativeFields(String body) throws Exception {
		mvc.perform(patch("/api/v1/categories/21").requestAttr("currentPrincipal", merchantPrincipal(2))
				.contentType(JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void categoryAndProductEndpointsRejectNonPositivePathIds() throws Exception {
		mvc.perform(get("/api/v1/shops/0/categories"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(patch("/api/v1/categories/0").requestAttr("currentPrincipal", merchantPrincipal(2)).contentType(JSON)
				.content("{\"name\":\"Meals\"}"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		mvc.perform(get("/api/v1/products/0"))
				.andExpect(status().isBadRequest()).andExpect(validationError());
		verifyNoInteractions(service);
	}

	@Test
	void merchantMutationsRejectMissingPrincipal() throws Exception {
		mvc.perform(post("/api/v1/shops/10/categories").contentType(JSON).content("{\"name\":\"Meals\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(patch("/api/v1/categories/21").contentType(JSON).content("{\"name\":\"Meals\"}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(delete("/api/v1/categories/21"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(post("/api/v1/products").contentType(JSON)
				.content("{\"shopId\":10,\"categoryId\":21,\"name\":\"Rice\",\"price\":1,\"stock\":0}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		mvc.perform(patch("/api/v1/products/30").contentType(JSON).content("{\"stock\":5}"))
				.andExpect(status().isUnauthorized()).andExpect(unauthenticated());
		verifyNoInteractions(service);
	}
}
