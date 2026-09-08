package com.delivery.backend;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.delivery.backend.common.PageResult;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

public final class TestFixtures {

	private static final Instant TIME = Instant.parse("2026-09-04T00:00:00Z");

	private TestFixtures() {
	}

	public static UserService.UserView user(long id) {
		return new UserService.UserView(id, "alice", "Alice", null, "ACTIVE", TIME, TIME);
	}

	public static UserService.AuthSession userSession(long id) {
		return new UserService.AuthSession("token", "Bearer", 7200, user(id), List.of("USER"));
	}

	public static MerchantService.MerchantView merchant(long id) {
		return new MerchantService.MerchantView(id, "merchant", "Store", "13900000000", "ACTIVE", TIME, TIME);
	}

	public static MerchantService.AuthSession merchantSession(long id) {
		return new MerchantService.AuthSession("token", "Bearer", 7200, merchant(id), List.of("MERCHANT"));
	}

	public static RestaurantService.ShopView shop(long id) {
		return new RestaurantService.ShopView(id, 2, "Shop", null, "OPEN", TIME, TIME);
	}

	public static PageResult<RestaurantService.ShopView> shopPage() {
		return new PageResult<>(List.of(shop(10)), 1, 10, 1, 1);
	}

	public static ItemService.CategoryView category(long id) {
		return new ItemService.CategoryView(id, 10, "Meals", 0, TIME, TIME);
	}

	public static ItemService.ProductView product(long id) {
		return new ItemService.ProductView(id, 10, 21, "Rice", null, new BigDecimal("12.50"), 5,
				"ON_SALE", 3, TIME, TIME);
	}

	public static PageResult<ItemService.ProductView> productPage() {
		return new PageResult<>(List.of(product(30)), 1, 10, 1, 1);
	}

	public static ShoppingService.CartItemView cartItem(long id) {
		ShoppingService.CartProductView product = new ShoppingService.CartProductView(30, 10, "Rice",
				new BigDecimal("12.50"), 5, "ON_SALE", 3);
		return new ShoppingService.CartItemView(id, product, 1, new BigDecimal("12.50"), true, null,
				TIME, TIME);
	}

	public static ShoppingService.CartView cart() {
		return new ShoppingService.CartView(List.of(cartItem(31)), new BigDecimal("12.50"));
	}

	public static OrderService.OrderView order(long id) {
		OrderService.OrderLineView line = new OrderService.OrderLineView(30, "Rice", new BigDecimal("12.50"),
				1, new BigDecimal("12.50"));
		return new OrderService.OrderView(id, "ORDER-40", 7, 10, "Shop", List.of(line),
				new BigDecimal("12.50"), "PENDING_PAYMENT", TIME, TIME, null);
	}

	public static PageResult<OrderService.OrderSummaryView> orderPage() {
		OrderService.OrderSummaryView summary = new OrderService.OrderSummaryView(40, "ORDER-40", 10, "Shop",
				new BigDecimal("12.50"), "PENDING_PAYMENT", TIME);
		return new PageResult<>(List.of(summary), 1, 10, 1, 1);
	}
}
