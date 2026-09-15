package com.delivery.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.item.service.SkuService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Transactional
class OrderPersistenceRedTests extends ServiceContractTestSupport {

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
	private SkuService skuService;
	@Autowired
	private ShoppingService shoppingService;
	@Autowired
	private UserAddressService addressService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SqlSession sqlSession;

	@Test
	void pendingPaymentCancellationDoesNotCreateARefund() {
		Fixture fixture = fixture("cancel-pending");
		OrderService.OrderView order = createOrder(fixture, "create-pending", null);

		OrderService.OrderView cancelled = orderService.cancel(fixture.userId(), order.id(),
				"cancel-pending-key", "  临时有事  ");

		assertThat(cancelled.status()).isEqualTo("CANCELLED");
		assertThat(cancelled.paymentStatus()).isEqualTo("UNPAID");
		assertThat(cancelled.refundStatus()).isEqualTo("NOT_REFUNDED");
		assertThat(cancelled.cancelReason()).isEqualTo("临时有事");
		assertThat(refundCount(order.id())).isZero();
		assertThat(skuStock(fixture.skuId())).isEqualTo(5);
	}

	@Test
	void paidCancellationCreatesExactlyOneFullRefund() {
		Fixture fixture = fixture("cancel-paid");
		OrderService.OrderView order = createOrder(fixture, "create-paid", null);
		orderService.pay(fixture.userId(), order.id(), "pay-paid-key");

		OrderService.OrderView cancelled = orderService.cancel(fixture.userId(), order.id(),
				"cancel-paid-key", null);
		OrderService.OrderView replay = orderService.cancel(fixture.userId(), order.id(),
				"cancel-paid-key", null);

		assertThat(cancelled.status()).isEqualTo("CANCELLED");
		assertThat(cancelled.paymentStatus()).isEqualTo("PAID");
		assertThat(cancelled.refundStatus()).isEqualTo("REFUNDED");
		assertThat(replay.id()).isEqualTo(cancelled.id());
		assertThat(refundCount(order.id())).isEqualTo(1);
		assertThat(refundAmount(order.id())).isEqualByComparingTo(order.total());
		assertThat(refundPaymentId(order.id())).isNotNull();
		assertThat(skuStock(fixture.skuId())).isEqualTo(5);
	}

	@Test
	void preparingCancellationCreatesExactlyOneFullRefund() {
		Fixture fixture = fixture("cancel-preparing");
		OrderService.OrderView order = createOrder(fixture, "create-preparing", null);
		orderService.pay(fixture.userId(), order.id(), "pay-preparing-key");
		orderService.prepare(fixture.merchantId(), order.id(), "prepare-key");

		OrderService.OrderView cancelled = orderService.cancel(fixture.userId(), order.id(),
				"cancel-preparing-key", "取消制作");

		assertThat(cancelled.status()).isEqualTo("CANCELLED");
		assertThat(cancelled.paymentStatus()).isEqualTo("PAID");
		assertThat(cancelled.refundStatus()).isEqualTo("REFUNDED");
		assertThat(refundCount(order.id())).isEqualTo(1);
		assertThat(refundAmount(order.id())).isEqualByComparingTo(order.total());
		assertThat(refundPaymentId(order.id())).isNotNull();
		assertThat(skuStock(fixture.skuId())).isEqualTo(5);
	}

	@Test
	void paidCancellationRollsBackWhenPaymentRecordIsMissing() {
		Fixture fixture = fixture("cancel-missing-payment");
		OrderService.OrderView order = createOrder(fixture, "create-missing-payment", null);
		orderService.pay(fixture.userId(), order.id(), "pay-before-delete");
		jdbcTemplate.update("DELETE FROM payments WHERE order_id=?", order.id());
		sqlSession.clearCache();

		assertThatThrownBy(() -> orderService.cancel(fixture.userId(), order.id(),
				"cancel-without-payment", null)).isInstanceOf(BusinessException.class);
		assertThat(orderStatus(order.id())).isEqualTo("PAID");
		assertThat(refundCount(order.id())).isZero();
		assertThat(skuStock(fixture.skuId())).isEqualTo(3);
	}

	@Test
	void sameMerchantCannotReusePrepareKeyForAnotherOrder() {
		Fixture fixture = fixture("prepare-key-scope");
		OrderService.OrderView first = createOrder(fixture, "create-first", null);
		OrderRef second = createAdditionalOrder(fixture, "prepare-second");
		orderService.pay(fixture.userId(), first.id(), "pay-first");
		orderService.pay(second.userId(), second.orderId(), "pay-second");
		orderService.prepare(fixture.merchantId(), first.id(), "shared-prepare-key");

		assertBusinessError(ApiError.IDEMPOTENCY_CONFLICT,
				() -> orderService.prepare(fixture.merchantId(), second.orderId(), "shared-prepare-key"));
		assertThat(orderStatus(second.orderId())).isEqualTo("PAID");
	}

	@Test
	void differentUsersMayUseTheSamePaymentKeyText() {
		Fixture firstFixture = fixture("pay-user-one");
		Fixture secondFixture = fixture("pay-user-two");
		OrderService.OrderView first = createOrder(firstFixture, "create-user-one", null);
		OrderService.OrderView second = createOrder(secondFixture, "create-user-two", null);

		OrderService.OrderView firstPaid = orderService.pay(firstFixture.userId(), first.id(), "shared-pay-key");
		OrderService.OrderView secondPaid = orderService.pay(secondFixture.userId(), second.id(), "shared-pay-key");

		assertThat(firstPaid.status()).isEqualTo("PAID");
		assertThat(secondPaid.status()).isEqualTo("PAID");
		assertThat(paymentCount(first.id()) + paymentCount(second.id())).isEqualTo(2);
	}

	@Test
	void idempotentReplayReturnsOriginalOrderAfterAddressDeletion() {
		Fixture fixture = fixture("replay-deleted-address");
		OrderService.CreateRequest request = request(fixture, null);
		OrderService.OrderView first = orderService.create(fixture.userId(), "replay-address-key", request);
		addressService.remove(fixture.userId(), fixture.addressId());

		OrderService.OrderView replay = orderService.create(fixture.userId(), "replay-address-key", request);

		assertThat(replay.id()).isEqualTo(first.id());
	}

	@Test
	void blankAndNullRemarkShareTheSameIdempotencyFingerprint() {
		Fixture fixture = fixture("remark-blank-null");
		OrderService.OrderView first = createOrder(fixture, "remark-equivalent-key", "   ");

		OrderService.OrderView replay = orderService.create(fixture.userId(), "remark-equivalent-key",
				request(fixture, null));

		assertThat(first.remark()).isNull();
		assertThat(replay.id()).isEqualTo(first.id());
	}

	@Test
	void normalizedRemarkProducesTheSameFingerprintAndUsesTrimmedLength() {
		Fixture fixture = fixture("remark-normalized");
		String twoHundredCharacters = "x".repeat(200);
		OrderService.OrderView first = createOrder(fixture, "remark-normalized-key",
				"   " + twoHundredCharacters + "   ");
		OrderService.OrderView replay = orderService.create(fixture.userId(), "remark-normalized-key",
				request(fixture, twoHundredCharacters));

		assertThat(first.remark()).isEqualTo(twoHundredCharacters);
		assertThat(first.remark()).hasSize(200);
		assertThat(replay.id()).isEqualTo(first.id());
	}

	private Fixture fixture(String label) {
		String suffix = label + "-" + System.nanoTime();
		long userId = createUser("u-" + suffix);
		long merchantId = merchantService.register(new MerchantService.RegisterRequest("m-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "Merchant", "13900000000")).id();
		RestaurantService.ShopView shop = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + suffix, null));
		restaurantService.updateAddress(merchantId, shop.id(),
				new RestaurantService.AddressRequest("杭州", "学院路", "05711234567"));
		RestaurantService.UpdateRequest open = new RestaurantService.UpdateRequest();
		open.setStatus("OPEN");
		restaurantService.update(merchantId, shop.id(), open);
		long categoryId = itemService.createCategory(merchantId, shop.id(),
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		long imageId = insertImage(merchantId);
		ItemService.ProductView product = itemService.createProduct(merchantId,
				new ItemService.CreateProductRequest(shop.id(), categoryId, "Rice", null,
						new BigDecimal("12.50"), 5, imageId,
						List.of(new SkuService.CreateRequest("默认规格", new BigDecimal("12.50"), 5))));
		ItemService.UpdateProductRequest productUpdate = new ItemService.UpdateProductRequest();
		productUpdate.setStatus("ON_SALE");
		productUpdate.setVersion(product.version());
		product = itemService.updateProduct(merchantId, product.id(), productUpdate);
		SkuService.UpdateRequest skuUpdate = new SkuService.UpdateRequest();
		skuUpdate.setStatus("ON_SALE");
		skuUpdate.setVersion(product.skus().get(0).version());
		SkuService.SkuView sku = skuService.update(merchantId, product.skus().get(0).id(), skuUpdate);
		long cartItemId = shoppingService.add(userId, new ShoppingService.AddRequest(sku.id(), 2)).item().id();
		long addressId = createAddress(userId, "学院路");
		return new Fixture(userId, merchantId, product.id(), sku.id(), sku.version(), cartItemId, addressId);
	}

	private OrderRef createAdditionalOrder(Fixture fixture, String label) {
		long userId = createUser("u-" + label + "-" + System.nanoTime());
		SkuService.SkuView currentSku = skuService.list(fixture.productId(), true, fixture.merchantId())
				.stream().filter(sku -> sku.id() == fixture.skuId()).findFirst().orElseThrow();
		long cartItemId = shoppingService.add(userId,
				new ShoppingService.AddRequest(fixture.skuId(), 1)).item().id();
		long addressId = createAddress(userId, "文一路");
		OrderService.CreateRequest request = new OrderService.CreateRequest(
				List.of(new OrderService.ItemRequest(cartItemId, currentSku.version())), addressId, null);
		long orderId = orderService.create(userId, "create-" + label, request).id();
		return new OrderRef(userId, orderId);
	}

	private OrderService.OrderView createOrder(Fixture fixture, String key, String remark) {
		return orderService.create(fixture.userId(), key, request(fixture, remark));
	}

	private static OrderService.CreateRequest request(Fixture fixture, String remark) {
		return new OrderService.CreateRequest(
				List.of(new OrderService.ItemRequest(fixture.cartItemId(), fixture.skuVersion())),
				fixture.addressId(), remark);
	}

	private long createUser(String account) {
		return userService.register(new UserService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "User", null)).id();
	}

	private long createAddress(long userId, String detail) {
		return addressService.create(userId,
				new UserAddressService.CreateRequest("张三", "13800000000", "杭州", detail, true)).id();
	}

	private long insertImage(long merchantId) {
		jdbcTemplate.update("INSERT INTO images(merchant_id,url,content_type,size) VALUES(?, '/uploads/test.webp','image/webp',4)", merchantId);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private int skuStock(long skuId) {
		return jdbcTemplate.queryForObject("SELECT stock FROM product_skus WHERE id=?", Integer.class, skuId);
	}

	private int refundCount(long orderId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds WHERE order_id=?", Integer.class, orderId);
	}

	private BigDecimal refundAmount(long orderId) {
		return jdbcTemplate.queryForObject("SELECT amount FROM refunds WHERE order_id=?", BigDecimal.class, orderId);
	}

	private Long refundPaymentId(long orderId) {
		return jdbcTemplate.queryForObject("SELECT payment_id FROM refunds WHERE order_id=?", Long.class, orderId);
	}

	private int paymentCount(long orderId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id=?", Integer.class, orderId);
	}

	private String orderStatus(long orderId) {
		return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id=?", String.class, orderId);
	}

	private record Fixture(long userId, long merchantId, long productId, long skuId, long skuVersion,
			long cartItemId, long addressId) {
	}

	private record OrderRef(long userId, long orderId) {
	}
}
