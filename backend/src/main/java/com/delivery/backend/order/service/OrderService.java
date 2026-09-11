package com.delivery.backend.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.PageResult;
import com.delivery.backend.shopping.service.ShoppingService;

/** Order business contract. Implementation and persistence are intentionally pending. */
public interface OrderService {

	OrderView create(long userId, String idempotencyKey, CreateRequest request);

	PageResult<OrderSummaryView> listMine(long userId, ListQuery query);

	OrderView getMine(long userId, long orderId);

	OrderView cancel(long userId, long orderId);

	default OrderView cancel(long userId, long orderId, String idempotencyKey, String reason) {
		return cancel(userId, orderId);
	}

	OrderView pay(long userId, long orderId, String idempotencyKey);

	OrderView confirmReceipt(long userId, long orderId, String idempotencyKey);

	OrderView prepare(long merchantId, long orderId, String idempotencyKey);

	OrderView deliver(long merchantId, long orderId, String idempotencyKey);

	RefundView getRefund(long userId, long orderId);

	PageResult<OrderSummaryView> listMerchantOrders(long merchantId, MerchantListQuery query);

	OrderView getMerchantOrder(long merchantId, long orderId);

	record CreateRequest(@NotEmpty List<@NotNull @Valid ItemRequest> items, @Positive Long addressId, String remark) {
		public CreateRequest(List<ItemRequest> items) {
			this(items, null, null);
		}

		public CreateRequest {
			items = items == null ? null : List.copyOf(items);
			remark = remark == null ? null : remark.trim();
		}
	}

	record ItemRequest(@Positive long cartItemId, @Positive long skuVersion) {
		public long productVersion() {
			return skuVersion;
		}
	}

	record ListQuery(String status, Integer page, Integer pageSize, String sortBy, String sortOrder) {
	}

	record MerchantListQuery(Long shopId, String status, Integer page, Integer pageSize, String sortBy,
		String sortOrder) {
	}

	record OrderLineView(long productId, Long skuId, String productName, String skuName, String imageUrl,
			BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
		public OrderLineView(long productId, String productName, BigDecimal unitPrice, int quantity,
				BigDecimal subtotal) {
			this(productId, null, productName, null, null, unitPrice, quantity, subtotal);
		}
	}

	record OrderView(long id, String orderNumber, long userId, long shopId, String shopName,
			List<OrderLineView> lines, BigDecimal total, String status, Instant createdAt, Instant updatedAt,
			Instant cancelledAt, String paymentStatus, String refundStatus, String remark, String cancelReason,
			Instant completedAt, Map<String, String> userAddressSnapshot, Map<String, String> shopAddressSnapshot) {
		public OrderView(long id, String orderNumber, long userId, long shopId, String shopName,
				List<OrderLineView> lines, BigDecimal total, String status, Instant createdAt, Instant updatedAt,
				Instant cancelledAt) {
			this(id, orderNumber, userId, shopId, shopName, lines, total, status, createdAt, updatedAt,
					cancelledAt, "UNPAID", "NOT_REFUNDED", null, null, null, null, null);
		}
		public OrderView {
			lines = List.copyOf(lines);
		}
	}


	record OrderSummaryView(long id, String orderNumber, long shopId, String shopName, BigDecimal total,
			String status, String paymentStatus, String refundStatus, Instant createdAt) {
		public OrderSummaryView(long id, String orderNumber, long shopId, String shopName, BigDecimal total,
				String status, Instant createdAt) {
			this(id, orderNumber, shopId, shopName, total, status, "UNPAID", "NOT_REFUNDED", createdAt);
		}
	}

	record RefundView(long orderId, String refundNumber, BigDecimal amount, String status) {}

	record PriceChangeData(List<ShoppingService.CartItemView> currentItems) {
		public PriceChangeData {
			currentItems = List.copyOf(currentItems);
		}
	}
}
