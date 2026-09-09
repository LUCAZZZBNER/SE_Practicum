package com.delivery.backend.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
class OrderConcurrencyTests {

	@Autowired
	private OrderService orderService;
	@Autowired
	private UserService userService;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private ShoppingService shoppingService;

	@Test
	void concurrentRetriesWithTheSameKeyReturnTheSameOrder() throws Exception {
		Fixture fixture = fixture("same-key");
		String key = "key-" + System.nanoTime();
		List<Attempt> attempts = runConcurrently(
				() -> orderService.create(fixture.userId(), key, fixture.request()),
				() -> orderService.create(fixture.userId(), key, fixture.request()));

		assertThat(attempts).allMatch(attempt -> attempt.failure() == null);
		assertThat(attempts).extracting(attempt -> attempt.order().id()).containsOnly(attempts.get(0).order().id());
		assertThat(orderService.listMine(fixture.userId(), query()).total()).isEqualTo(1);
		assertThat(product(fixture).stock()).isEqualTo(8);
	}

	@Test
	void concurrentDifferentOrdersCannotConsumeTheSameCartItemTwice() throws Exception {
		Fixture fixture = fixture("different-keys");
		List<Attempt> attempts = runConcurrently(
				() -> orderService.create(fixture.userId(), "first-" + System.nanoTime(), fixture.request()),
				() -> orderService.create(fixture.userId(), "second-" + System.nanoTime(), fixture.request()));

		assertThat(attempts).filteredOn(attempt -> attempt.failure() == null).hasSize(1);
		assertThat(attempts).filteredOn(attempt -> attempt.failure() instanceof BusinessException)
				.singleElement().satisfies(attempt -> assertThat(((BusinessException) attempt.failure()).error())
						.isEqualTo(ApiError.CART_EMPTY));
		assertThat(orderService.listMine(fixture.userId(), query()).total()).isEqualTo(1);
		assertThat(product(fixture).stock()).isEqualTo(8);
	}

	private Fixture fixture(String label) {
		String suffix = label + "-" + System.nanoTime();
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
		ShoppingService.CartItemView cartItem = shoppingService.add(userId,
				new ShoppingService.AddRequest(product.id(), 2)).item();
		OrderService.CreateRequest request = new OrderService.CreateRequest(
				List.of(new OrderService.ItemRequest(cartItem.id(), product.version())));
		return new Fixture(userId, merchantId, product.id(), request);
	}

	private ItemService.ProductView product(Fixture fixture) {
		return itemService.getProduct(fixture.productId(), true, fixture.merchantId());
	}

	private static OrderService.ListQuery query() {
		return new OrderService.ListQuery(null, 1, 10, null, null);
	}

	private static List<Attempt> runConcurrently(OrderCall first, OrderCall second) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<OrderService.OrderView> firstFuture = executor.submit(() -> callAfter(start, first));
			Future<OrderService.OrderView> secondFuture = executor.submit(() -> callAfter(start, second));
			start.countDown();
			return List.of(attempt(firstFuture), attempt(secondFuture));
		} finally {
			executor.shutdownNow();
		}
	}

	private static OrderService.OrderView callAfter(CountDownLatch start, OrderCall call) {
		try {
			start.await();
			return call.run();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private static Attempt attempt(Future<OrderService.OrderView> future) throws Exception {
		try {
			return new Attempt(future.get(), null);
		} catch (ExecutionException exception) {
			return new Attempt(null, exception.getCause());
		}
	}

	@FunctionalInterface
	private interface OrderCall {
		OrderService.OrderView run();
	}

	private record Attempt(OrderService.OrderView order, Throwable failure) {
	}

	private record Fixture(long userId, long merchantId, long productId, OrderService.CreateRequest request) {
	}
}
