package com.delivery.backend.order;

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
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
@Transactional
class OrderServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private OrderService service;
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
	void createCalculatesServerTotalStoresSnapshotsAndRemovesSelectedCartItems() {
		Fixture fixture = fixture("order-create");
		OrderService.OrderView order = service.create(fixture.userId(), "order-create-key", request(fixture));

		assertThat(order.id()).isPositive();
		assertThat(order.total()).isEqualByComparingTo("25.00");
		assertThat(order.status()).isEqualTo("PENDING_PAYMENT");
		assertThat(order.lines()).singleElement().satisfies(line -> {
			assertThat(line.productName()).isEqualTo("Rice");
			assertThat(line.unitPrice()).isEqualByComparingTo("12.50");
			assertThat(line.quantity()).isEqualTo(2);
		});
		assertThat(shoppingService.getCart(fixture.userId()).items()).isEmpty();
	}

	@Test
	void sameIdempotencyRequestReturnsOriginalOrderAndDifferentRequestConflicts() {
		Fixture fixture = fixture("order-idempotency");
		OrderService.CreateRequest request = request(fixture);
		OrderService.OrderView first = service.create(fixture.userId(), "same-key", request);

		assertThat(service.create(fixture.userId(), "same-key", request).id()).isEqualTo(first.id());
		assertBusinessError(ApiError.IDEMPOTENCY_CONFLICT, () -> service.create(fixture.userId(), "same-key",
				new OrderService.CreateRequest(List.of(
						new OrderService.ItemRequest(fixture.cartItemId(), fixture.productVersion() + 1)))));
	}

	@Test
	void emptyCartAndChangedProductVersionFailWithoutRemovingCartOrReducingStock() {
		Fixture fixture = fixture("order-failure");
		assertBusinessError(ApiError.CART_EMPTY,
				() -> service.create(fixture.userId(), "empty-key", new OrderService.CreateRequest(List.of())));
		assertBusinessError(ApiError.PRICE_CHANGED, () -> service.create(fixture.userId(), "changed-key",
				new OrderService.CreateRequest(List.of(
						new OrderService.ItemRequest(fixture.cartItemId(), fixture.productVersion() - 1)))));

		assertThat(shoppingService.getCart(fixture.userId()).items()).extracting(ShoppingService.CartItemView::id)
				.contains(fixture.cartItemId());
		assertThat(itemService.getProduct(fixture.productId(), true, fixture.merchantId()).stock()).isEqualTo(5);
	}

	@Test
	void userAndMerchantQueriesEnforceOwnership() {
		Fixture fixture = fixture("order-query");
		OrderService.OrderView order = service.create(fixture.userId(), "query-key", request(fixture));
		long otherUserId = user("order-query-other").id();
		long otherMerchantId = merchant("order-query-other").id();

		assertThat(service.getMine(fixture.userId(), order.id()).id()).isEqualTo(order.id());
		assertThat(service.listMine(fixture.userId(), new OrderService.ListQuery(null, 1, 10, null, null)).items())
				.extracting(OrderService.OrderSummaryView::id).contains(order.id());
		assertThat(service.listMerchantOrders(fixture.merchantId(),
				new OrderService.MerchantListQuery(fixture.shopId(), null, 1, 10, null, null)).items())
				.extracting(OrderService.OrderSummaryView::id).contains(order.id());
		assertThat(service.getMerchantOrder(fixture.merchantId(), order.id()).id()).isEqualTo(order.id());
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND, () -> service.getMine(otherUserId, order.id()));
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND,
				() -> service.getMerchantOrder(otherMerchantId, order.id()));
	}

	@Test
	void cancellationChangesOnlyPendingOrderAndRestoresStockOnce() {
		Fixture fixture = fixture("order-cancel");
		OrderService.OrderView order = service.create(fixture.userId(), "cancel-key", request(fixture));
		assertThat(itemService.getProduct(fixture.productId(), true, fixture.merchantId()).stock()).isEqualTo(3);

		OrderService.OrderView cancelled = service.cancel(fixture.userId(), order.id());
		assertThat(cancelled.status()).isEqualTo("CANCELLED");
		assertThat(cancelled.cancelledAt()).isNotNull();
		assertThat(itemService.getProduct(fixture.productId(), true, fixture.merchantId()).stock()).isEqualTo(5);
		assertBusinessError(ApiError.ORDER_STATE_CONFLICT, () -> service.cancel(fixture.userId(), order.id()));
		assertThat(itemService.getProduct(fixture.productId(), true, fixture.merchantId()).stock()).isEqualTo(5);
	}

	private Fixture fixture(String name) {
		long userId = user(name + "-user").id();
		long merchantId = merchant(name + "-merchant").id();
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
		ShoppingService.CartItemView cartItem = shoppingService.add(userId,
				new ShoppingService.AddRequest(product.id(), 2)).item();
		return new Fixture(userId, merchantId, shop.id(), product.id(), product.version(), cartItem.id());
	}

	private UserService.UserView user(String account) {
		return userService.register(new UserService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "Alice", null));
	}

	private MerchantService.MerchantView merchant(String account) {
		return merchantService.register(new MerchantService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "Store", "13900000000"));
	}

	private static OrderService.CreateRequest request(Fixture fixture) {
		return new OrderService.CreateRequest(List.of(
				new OrderService.ItemRequest(fixture.cartItemId(), fixture.productVersion())));
	}

	private record Fixture(long userId, long merchantId, long shopId, long productId, long productVersion,
			long cartItemId) {
	}
}
