package com.delivery.backend.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
class ShoppingConcurrencyTests {

	@Autowired
	private ShoppingService shoppingService;
	@Autowired
	private UserService userService;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private ItemService itemService;

	@Test
	void concurrentAddsIncrementInsteadOfOverwritingQuantity() throws Exception {
		Fixture fixture = fixture();
		shoppingService.add(fixture.userId(), new ShoppingService.AddRequest(fixture.productId(), 1));
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> first = executor.submit(() -> addAfter(start, fixture));
			Future<?> second = executor.submit(() -> addAfter(start, fixture));
			start.countDown();
			first.get();
			second.get();
		} finally {
			executor.shutdownNow();
		}

		assertThat(shoppingService.getCart(fixture.userId()).items()).singleElement()
				.extracting(ShoppingService.CartItemView::quantity).isEqualTo(3);
	}

	private Fixture fixture() {
		String suffix = Long.toString(System.nanoTime());
		long userId = userService.register(new UserService.RegisterRequest("u-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "User", null)).id();
		long merchantId = merchantService.register(new MerchantService.RegisterRequest("m-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "Merchant", "13900000000")).id();
		RestaurantService.ShopView shop = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + suffix, null));
		RestaurantService.UpdateRequest open = new RestaurantService.UpdateRequest();
		open.setStatus("OPEN");
		restaurantService.update(merchantId, shop.id(), open);
		long categoryId = itemService.createCategory(merchantId, shop.id(),
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		ItemService.ProductView product = itemService.createProduct(merchantId,
				new ItemService.CreateProductRequest(shop.id(), categoryId, "Rice", null,
						new BigDecimal("12.50"), 10));
		ItemService.UpdateProductRequest onSale = new ItemService.UpdateProductRequest();
		onSale.setStatus("ON_SALE");
		onSale.setVersion(product.version());
		product = itemService.updateProduct(merchantId, product.id(), onSale);
		return new Fixture(userId, product.id());
	}

	private void addAfter(CountDownLatch start, Fixture fixture) {
		try {
			start.await();
			shoppingService.add(fixture.userId(), new ShoppingService.AddRequest(fixture.productId(), 1));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private record Fixture(long userId, long productId) {
	}
}
