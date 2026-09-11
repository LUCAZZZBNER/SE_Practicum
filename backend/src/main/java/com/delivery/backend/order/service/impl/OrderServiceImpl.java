package com.delivery.backend.order.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.order.dao.OrderDao;
import com.delivery.backend.order.entity.OrderEntity;
import com.delivery.backend.order.entity.OrderItemEntity;
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

/** Transactional order creation, history, idempotency, and cancellation behavior. */
@Service
public class OrderServiceImpl implements OrderService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final String PENDING_PAYMENT = "PENDING_PAYMENT";
	private static final Set<String> ORDER_STATUSES = Set.of(
			PENDING_PAYMENT, "PAID", "PREPARING", "DELIVERING", "COMPLETED", "CANCELLED");

	private final OrderDao orderDao;
	private final UserService userService;
	private final MerchantService merchantService;
	private final RestaurantService restaurantService;
	private final ShoppingService shoppingService;
	private final ItemService itemService;
	private final UserAddressService addressService;
	private final ObjectMapper objectMapper;

	public OrderServiceImpl(OrderDao orderDao, UserService userService, MerchantService merchantService,
			RestaurantService restaurantService, ShoppingService shoppingService, ItemService itemService) {
		this(orderDao, userService, merchantService, restaurantService, shoppingService, itemService, null, new ObjectMapper());
	}

	@Autowired
	public OrderServiceImpl(OrderDao orderDao, UserService userService, MerchantService merchantService,
			RestaurantService restaurantService, ShoppingService shoppingService, ItemService itemService,
			UserAddressService addressService, ObjectMapper objectMapper) {
		this.orderDao = orderDao;
		this.userService = userService;
		this.merchantService = merchantService;
		this.restaurantService = restaurantService;
		this.shoppingService = shoppingService;
		this.itemService = itemService;
		this.addressService = addressService;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public OrderView create(long userId, String idempotencyKey, CreateRequest request) {
		userService.requireActiveForUpdate(userId);
		String key = normalizeIdempotencyKey(idempotencyKey);
		List<ItemRequest> requestedItems = validateItems(request);
		if (request.addressId() == null || request.addressId() <= 0 || addressService == null) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		if (request.remark() != null && request.remark().length() > 200) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		UserAddressService.AddressView address = addressService.requireOwned(userId, request.addressId());
		String fingerprint = fingerprint(requestedItems);

		OrderEntity existing = orderDao.findByUserAndIdempotency(userId, key);
		if (existing != null) {
			if (!fingerprint.equals(existing.getRequestFingerprint())) {
				throw new BusinessException(ApiError.IDEMPOTENCY_CONFLICT);
			}
			return toOrderView(existing, orderDao.listItems(existing.getId()));
		}

		Map<Long, Long> versionsByCartItem = new HashMap<>();
		for (ItemRequest item : requestedItems) {
			versionsByCartItem.put(item.cartItemId(), item.productVersion());
		}
		List<Long> cartItemIds = requestedItems.stream().map(ItemRequest::cartItemId).toList();
		List<ShoppingService.CheckoutItem> checkoutItems = shoppingService.loadForCheckout(userId, cartItemIds);
		for (ShoppingService.CheckoutItem item : checkoutItems) {
			if (item.confirmedVersion() != versionsByCartItem.get(item.cartItemId())) {
				throw new BusinessException(ApiError.PRICE_CHANGED);
			}
		}
		long shopId = singleShopId(checkoutItems);
		RestaurantService.ShopSnapshot shop = restaurantService.requireOrderable(shopId);

		List<ItemService.ReservationRequest> reservations = checkoutItems.stream()
				.map(item -> new ItemService.ReservationRequest(item.productId(),
						versionsByCartItem.get(item.cartItemId()), item.quantity()))
				.sorted(Comparator.comparingLong(ItemService.ReservationRequest::productId))
				.toList();
		List<ItemService.ProductSnapshot> snapshots = itemService.reserveForOrder(reservations);
		BigDecimal total = snapshots.stream()
				.map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		OrderEntity order = new OrderEntity();
		order.setOrderNumber("ORD" + UUID.randomUUID().toString().replace("-", ""));
		order.setUserId(userId);
		order.setIdempotencyKey(key);
		order.setRequestFingerprint(fingerprint);
		order.setShopId(shop.id());
		order.setShopName(shop.name());
		order.setTotalAmount(total);
		order.setStatus(PENDING_PAYMENT);
		order.setPaymentStatus("UNPAID");
		order.setRefundStatus("NOT_REFUNDED");
		order.setRemark(normalizeReason(request.remark()));
		order.setUserAddressSnapshot(writeSnapshot(new OrderService.AddressSnapshot(address.recipient(), address.phone(), address.region(), address.detail())));
		order.setShopAddressSnapshot(writeSnapshot(new OrderService.ShopAddressSnapshot(shop.region(), shop.detail(), shop.phone())));
		orderDao.insertOrder(order);

		List<OrderItemEntity> lines = snapshots.stream().map(snapshot -> toEntity(order.getId(), snapshot)).toList();
		orderDao.insertItems(lines);
		shoppingService.removeAfterCheckout(userId, cartItemIds);
		return requireMine(userId, order.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<OrderSummaryView> listMine(long userId, ListQuery query) {
		userService.requireActive(userId);
		String status = validateStatus(query.status());
		int page = defaultPage(query.page());
		int pageSize = defaultPageSize(query.pageSize());
		String sortBy = validateSortBy(query.sortBy());
		String sortOrder = validateSortOrder(query.sortOrder());
		long offset = (long) (page - 1) * pageSize;
		List<OrderSummaryView> items = orderDao.listMine(userId, status, sortBy, sortOrder, pageSize, offset)
				.stream().map(OrderServiceImpl::toSummaryView).toList();
		long total = orderDao.countMine(userId, status);
		return page(items, page, pageSize, total);
	}

	@Override
	@Transactional(readOnly = true)
	public OrderView getMine(long userId, long orderId) {
		userService.requireActive(userId);
		return requireMine(userId, orderId);
	}

	@Override
	@Transactional
	public OrderView cancel(long userId, long orderId) {
		return cancel(userId, orderId, null, null);
	}

	@Override
	@Transactional
	public OrderView cancel(long userId, long orderId, String idempotencyKey, String reason) {
		userService.requireActive(userId);
		OrderEntity order = findMine(userId, orderId);
		if (!Set.of(PENDING_PAYMENT, "PAID", "PREPARING").contains(order.getStatus())) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		List<OrderItemEntity> lines = orderDao.listItems(orderId);
		boolean restore = !PENDING_PAYMENT.equals(order.getStatus());
		if (orderDao.cancelEligible(userId, orderId, normalizeReason(reason), restore) != 1) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		if (restore) itemService.restoreStock(lines.stream().map(line -> new ItemService.StockRestore(line.getProductId(), line.getQuantity())).toList());
		return requireMine(userId, orderId);
	}

	@Override
	@Transactional
	public OrderView pay(long userId, long orderId, String idempotencyKey) {
		userService.requireActive(userId);
		findMine(userId, orderId);
		if (orderDao.transitionStatus(orderId, "PAID", PENDING_PAYMENT) != 1) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		return requireMine(userId, orderId);
	}

	@Override
	@Transactional
	public OrderView confirmReceipt(long userId, long orderId, String idempotencyKey) {
		userService.requireActive(userId);
		findMine(userId, orderId);
		if (orderDao.transitionStatus(orderId, "COMPLETED", "DELIVERING") != 1) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		return requireMine(userId, orderId);
	}

	@Override
	@Transactional
	public OrderView prepare(long merchantId, long orderId, String idempotencyKey) {
		merchantService.getCurrent(merchantId);
		OrderEntity order = orderDao.findMerchantOrder(merchantId, orderId);
		if (order == null || orderDao.transitionStatus(orderId, "PREPARING", "PAID") != 1) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		return toOrderView(orderDao.findMerchantOrder(merchantId, orderId), orderDao.listItems(orderId));
	}

	@Override
	@Transactional
	public OrderView deliver(long merchantId, long orderId, String idempotencyKey) {
		merchantService.getCurrent(merchantId);
		OrderEntity order = orderDao.findMerchantOrder(merchantId, orderId);
		if (order == null || orderDao.transitionStatus(orderId, "DELIVERING", "PREPARING") != 1) {
			throw new BusinessException(ApiError.ORDER_STATE_CONFLICT);
		}
		return toOrderView(orderDao.findMerchantOrder(merchantId, orderId), orderDao.listItems(orderId));
	}

	@Override
	@Transactional(readOnly = true)
	public RefundView getRefund(long userId, long orderId) {
		userService.requireActive(userId);
		OrderEntity order = findMine(userId, orderId);
		if (!"REFUNDED".equals(order.getRefundStatus())) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		return new RefundView(orderId, "REFUND-" + orderId, order.getTotalAmount(), order.getRefundStatus());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<OrderSummaryView> listMerchantOrders(long merchantId, MerchantListQuery query) {
		merchantService.getCurrent(merchantId);
		if (query.shopId() != null) {
			restaurantService.requireOwnedForRead(merchantId, query.shopId());
		}
		String status = validateStatus(query.status());
		int page = defaultPage(query.page());
		int pageSize = defaultPageSize(query.pageSize());
		String sortBy = validateSortBy(query.sortBy());
		String sortOrder = validateSortOrder(query.sortOrder());
		long offset = (long) (page - 1) * pageSize;
		List<OrderSummaryView> items = orderDao.listMerchant(merchantId, query.shopId(), status,
				sortBy, sortOrder, pageSize, offset).stream().map(OrderServiceImpl::toSummaryView).toList();
		long total = orderDao.countMerchant(merchantId, query.shopId(), status);
		return page(items, page, pageSize, total);
	}

	@Override
	@Transactional(readOnly = true)
	public OrderView getMerchantOrder(long merchantId, long orderId) {
		merchantService.getCurrent(merchantId);
		OrderEntity order = orderDao.findMerchantOrder(merchantId, orderId);
		if (order == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return toOrderView(order, orderDao.listItems(orderId));
	}

	private OrderView requireMine(long userId, long orderId) {
		OrderEntity order = findMine(userId, orderId);
		return toOrderView(order, orderDao.listItems(orderId));
	}

	private OrderEntity findMine(long userId, long orderId) {
		OrderEntity order = orderDao.findMine(userId, orderId);
		if (order == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return order;
	}

	private static List<ItemRequest> validateItems(CreateRequest request) {
		if (request == null || request.items() == null || request.items().isEmpty()) {
			throw new BusinessException(ApiError.CART_EMPTY);
		}
		Set<Long> cartItemIds = new HashSet<>();
		for (ItemRequest item : request.items()) {
			if (item == null || item.cartItemId() <= 0 || item.productVersion() <= 0
					|| !cartItemIds.add(item.cartItemId())) {
				throw new BusinessException(ApiError.VALIDATION_ERROR);
			}
		}
		return request.items();
	}

	private static long singleShopId(List<ShoppingService.CheckoutItem> items) {
		long shopId = items.get(0).shopId();
		if (items.stream().anyMatch(item -> item.shopId() != shopId)) {
			throw new BusinessException(ApiError.MIXED_SHOPS);
		}
		return shopId;
	}

	private static String normalizeIdempotencyKey(String key) {
		if (key == null) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		String normalized = key.trim();
		if (normalized.isEmpty() || normalized.length() > 100) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return normalized;
	}

	private static String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) return null;
		String normalized = reason.trim();
		if (normalized.length() > 200) throw new BusinessException(ApiError.VALIDATION_ERROR);
		return normalized;
	}

	private String writeSnapshot(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to serialize order snapshot", exception);
		}
	}

	private <T> T readSnapshot(String value, Class<T> type) {
		if (value == null || value.isBlank()) return null;
		try {
			return objectMapper.readValue(value, type);
		} catch (JsonProcessingException exception) {
			return null;
		}
	}

	private static String fingerprint(List<ItemRequest> items) {
		List<ItemRequest> sorted = new ArrayList<>(items);
		sorted.sort(Comparator.comparingLong(ItemRequest::cartItemId));
		StringBuilder canonical = new StringBuilder();
		for (ItemRequest item : sorted) {
			canonical.append(item.cartItemId()).append(':').append(item.productVersion()).append(';');
		}
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static OrderItemEntity toEntity(long orderId, ItemService.ProductSnapshot snapshot) {
		OrderItemEntity item = new OrderItemEntity();
		item.setOrderId(orderId);
		item.setProductId(snapshot.productId());
		item.setProductName(snapshot.name());
		item.setUnitPrice(snapshot.unitPrice());
		item.setQuantity(snapshot.quantity());
		return item;
	}

	private static OrderView toOrderView(OrderEntity order, List<OrderItemEntity> items) {
		List<OrderLineView> lines = items.stream().map(item -> new OrderLineView(
				item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(),
				item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList();
		return new OrderView(order.getId(), order.getOrderNumber(), order.getUserId(), order.getShopId(),
				order.getShopName(), lines, order.getTotalAmount(), order.getStatus(), order.getCreatedAt(),
				order.getUpdatedAt(), order.getCancelledAt(), order.getPaymentStatus(), order.getRefundStatus(),
				order.getRemark(), order.getCancelReason(), order.getCompletedAt(),
				readSnapshot(order.getUserAddressSnapshot(), OrderService.AddressSnapshot.class),
				readSnapshot(order.getShopAddressSnapshot(), OrderService.ShopAddressSnapshot.class));
	}

	private static OrderSummaryView toSummaryView(OrderEntity order) {
		return new OrderSummaryView(order.getId(), order.getOrderNumber(), order.getShopId(),
				order.getShopName(), order.getTotalAmount(), order.getStatus(), order.getCreatedAt());
	}

	private static <T> PageResult<T> page(List<T> items, int page, int pageSize, long total) {
		int totalPages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
		return new PageResult<>(items, page, pageSize, total, totalPages);
	}

	private static int defaultPage(Integer page) {
		int result = page == null ? DEFAULT_PAGE : page;
		if (result < 1) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return result;
	}

	private static int defaultPageSize(Integer pageSize) {
		int result = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
		if (result < 1 || result > 100) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return result;
	}

	private static String validateStatus(String status) {
		if (status != null && !ORDER_STATUSES.contains(status)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return status;
	}

	private static String validateSortBy(String sortBy) {
		if (sortBy == null) {
			return "createdAt";
		}
		if (!"createdAt".equals(sortBy) && !"total".equals(sortBy)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return sortBy;
	}

	private static String validateSortOrder(String sortOrder) {
		if (sortOrder == null) {
			return "desc";
		}
		if (!"asc".equals(sortOrder) && !"desc".equals(sortOrder)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return sortOrder;
	}
}
