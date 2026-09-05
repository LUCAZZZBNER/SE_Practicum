package com.delivery.backend.shopping;

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
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
@Transactional
class ShoppingServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private ShoppingService service;
	@Autowired
	private UserService userService;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private ItemService itemService;

	@Test
	void addingTheSameProductCreatesOnceThenMergesQuantity() {
		Fixture fixture = fixture("cart-merge");
		ShoppingService.AddResult created = service.add(fixture.userId(),
				new ShoppingService.AddRequest(fixture.productId(), 1));
		ShoppingService.AddResult merged = service.add(fixture.userId(),
				new ShoppingService.AddRequest(fixture.productId(), 2));

		assertThat(created.created()).isTrue();
		assertThat(merged.created()).isFalse();
		assertThat(merged.item().id()).isEqualTo(created.item().id());
		assertThat(merged.item().quantity()).isEqualTo(3);
	}

	@Test
	void cartUsesLatestProductDataAndCalculatesDisplayTotal() {
		Fixture fixture = fixture("cart-total");
		service.add(fixture.userId(), new ShoppingService.AddRequest(fixture.productId(), 2));

		ShoppingService.CartView cart = service.getCart(fixture.userId());
		assertThat(cart.items()).singleElement().satisfies(item -> {
			assertThat(item.product().price()).isEqualByComparingTo("12.50");
			assertThat(item.subtotal()).isEqualByComparingTo("25.00");
			assertThat(item.available()).isTrue();
		});
		assertThat(cart.total()).isEqualByComparingTo("25.00");
	}

	@Test
	void quantityChangesAndDeletionAreRestrictedToTheOwningUser() {
		Fixture fixture = fixture("cart-owner");
		long otherUserId = user("cart-other").id();
		ShoppingService.CartItemView item = service.add(fixture.userId(),
				new ShoppingService.AddRequest(fixture.productId(), 1)).item();

		assertThat(service.changeQuantity(fixture.userId(), item.id(), 2).quantity()).isEqualTo(2);
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND,
				() -> service.changeQuantity(otherUserId, item.id(), 2));
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND, () -> service.remove(otherUserId, item.id()));
		assertThat(service.remove(fixture.userId(), item.id()).deleted()).isTrue();
	}

	@Test
	void checkoutLoadingReturnsOnlySelectedOwnedItemsAndSuccessfulRemovalIsSelective() {
		Fixture fixture = fixture("cart-checkout");
		ShoppingService.CartItemView selected = service.add(fixture.userId(),
				new ShoppingService.AddRequest(fixture.productId(), 1)).item();
		List<ShoppingService.CheckoutItem> checkout = service.loadForCheckout(fixture.userId(),
				List.of(selected.id()));
		assertThat(checkout).singleElement().satisfies(item -> {
			assertThat(item.cartItemId()).isEqualTo(selected.id());
			assertThat(item.productId()).isEqualTo(fixture.productId());
		});

		service.removeAfterCheckout(fixture.userId(), List.of(selected.id()));
		assertThat(service.getCart(fixture.userId()).items()).isEmpty();
		assertBusinessError(ApiError.CART_EMPTY,
				() -> service.loadForCheckout(fixture.userId(), List.of(selected.id())));
	}

	private Fixture fixture(String name) {
		long userId = user(name + "-user").id();
		long merchantId = merchantService.register(new MerchantService.RegisterRequest(name + "-merchant",
				"ExamplePass123!", "ExamplePass123!", "Store", "13900000000")).id();
		RestaurantService.ShopView shop = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + name, null));
		RestaurantService.UpdateRequest open = new RestaurantService.UpdateRequest();
		open.setStatus("OPEN");
		restaurantService.update(merchantId, shop.id(), open);
		long categoryId = itemService.createCategory(merchantId, shop.id(),
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		ItemService.ProductView product = itemService.createProduct(merchantId,
				new ItemService.CreateProductRequest(shop.id(), categoryId, "Rice", null,
						new BigDecimal("12.50"), 5));
		ItemService.UpdateProductRequest onSale = new ItemService.UpdateProductRequest();
		onSale.setStatus("ON_SALE");
		onSale.setVersion(product.version());
		product = itemService.updateProduct(merchantId, product.id(), onSale);
		return new Fixture(userId, product.id());
	}

	private UserService.UserView user(String account) {
		return userService.register(new UserService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "Alice", null));
	}

	private record Fixture(long userId, long productId) {
	}
}
