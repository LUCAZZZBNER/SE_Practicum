package com.delivery.backend.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;

@SpringBootTest
@Transactional
class ItemServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private ItemService service;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;

	@Test
	void categoryCrudIsScopedToTheOwningShopAndUsesLogicalDeletion() {
		Fixture fixture = fixture("item-category");
		ItemService.CategoryView category = service.createCategory(fixture.merchantId(), fixture.shopId(),
				new ItemService.CreateCategoryRequest("Meals", 0));
		assertThat(service.listCategories(fixture.shopId())).extracting(ItemService.CategoryView::id)
				.contains(category.id());

		ItemService.UpdateCategoryRequest update = new ItemService.UpdateCategoryRequest();
		update.setName("Main Meals");
		assertThat(service.updateCategory(fixture.merchantId(), category.id(), update).name())
				.isEqualTo("Main Meals");
		assertThat(service.deleteCategory(fixture.merchantId(), category.id()).deleted()).isTrue();
	}

	@Test
	void productCreationDefaultsOffSaleAndPublicReadsHideOffSaleProducts() {
		Fixture fixture = fixture("item-product");
		ItemService.ProductView product = createProduct(fixture);
		assertThat(product.status()).isEqualTo("OFF_SALE");
		assertThat(product.version()).isPositive();
		assertThat(service.listProducts(fixture.shopId(),
				new ItemService.ProductQuery(null, null, 1, 10, null, null, false, null)).items())
				.noneMatch(item -> item.id() == product.id());
		assertThat(service.getProduct(product.id(), true, fixture.merchantId()).id()).isEqualTo(product.id());
	}

	@Test
	void productUpdateChecksOwnershipAndOptimisticVersion() {
		Fixture fixture = fixture("item-update");
		ItemService.ProductView product = createProduct(fixture);
		ItemService.UpdateProductRequest update = new ItemService.UpdateProductRequest();
		update.setPrice(new BigDecimal("13.00"));
		update.setVersion(product.version());
		ItemService.ProductView updated = service.updateProduct(fixture.merchantId(), product.id(), update);
		assertThat(updated.price()).isEqualByComparingTo("13.00");
		assertThat(updated.version()).isGreaterThan(product.version());
		assertBusinessError(ApiError.RESOURCE_CONFLICT,
				() -> service.updateProduct(fixture.merchantId(), product.id(), update));
	}

	@Test
	void reservationUsesExpectedVersionAndRejectsOffSaleOrInsufficientStock() {
		Fixture fixture = fixture("item-reserve");
		ItemService.ProductView product = createProduct(fixture);
		long offSaleProductId = product.id();
		long offSaleVersion = product.version();
		assertBusinessError(ApiError.PRODUCT_OFF_SALE, () -> service.reserveForOrder(
				List.of(new ItemService.ReservationRequest(offSaleProductId, offSaleVersion, 1))));

		ItemService.UpdateProductRequest onSale = new ItemService.UpdateProductRequest();
		onSale.setStatus("ON_SALE");
		onSale.setVersion(product.version());
		product = service.updateProduct(fixture.merchantId(), product.id(), onSale);
		long version = product.version();
		long productId = product.id();
		assertBusinessError(ApiError.INSUFFICIENT_STOCK, () -> service.reserveForOrder(
				List.of(new ItemService.ReservationRequest(productId, version, 6))));
		assertBusinessError(ApiError.PRICE_CHANGED, () -> service.reserveForOrder(
				List.of(new ItemService.ReservationRequest(productId, version - 1, 1))));
	}

	@Test
	void successfulReservationAndRestorationChangeStockExactlyOncePerCall() {
		Fixture fixture = fixture("item-stock");
		ItemService.ProductView product = createProduct(fixture);
		ItemService.UpdateProductRequest onSale = new ItemService.UpdateProductRequest();
		onSale.setStatus("ON_SALE");
		onSale.setVersion(product.version());
		product = service.updateProduct(fixture.merchantId(), product.id(), onSale);

		List<ItemService.ProductSnapshot> snapshots = service.reserveForOrder(
				List.of(new ItemService.ReservationRequest(product.id(), product.version(), 2)));
		assertThat(snapshots).singleElement().satisfies(snapshot -> {
			assertThat(snapshot.quantity()).isEqualTo(2);
			assertThat(snapshot.unitPrice()).isEqualByComparingTo("12.50");
		});
		service.restoreStock(List.of(new ItemService.StockRestore(product.id(), 2)));
		assertThat(service.getProduct(product.id(), true, fixture.merchantId()).stock()).isEqualTo(5);
	}

	private Fixture fixture(String account) {
		long merchantId = merchantService.register(new MerchantService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "Store", "13900000000")).id();
		long shopId = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + account, null)).id();
		long categoryId = service.createCategory(merchantId, shopId,
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		return new Fixture(merchantId, shopId, categoryId);
	}

	private ItemService.ProductView createProduct(Fixture fixture) {
		return service.createProduct(fixture.merchantId(), new ItemService.CreateProductRequest(fixture.shopId(),
				fixture.categoryId(), "Rice", null, new BigDecimal("12.50"), 5));
	}

	private record Fixture(long merchantId, long shopId, long categoryId) {
	}
}
