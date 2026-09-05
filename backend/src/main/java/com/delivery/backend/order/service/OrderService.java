package com.delivery.backend.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

	PageResult<OrderSummaryView> listMerchantOrders(long merchantId, MerchantListQuery query);

	OrderView getMerchantOrder(long merchantId, long orderId);

	record CreateRequest(@NotEmpty List<@NotNull @Valid ItemRequest> items) {
		public CreateRequest {
			items = List.copyOf(items);
		}
	}

	record ItemRequest(@Positive long cartItemId, @Positive long productVersion) {
	}

	record ListQuery(String status, Integer page, Integer pageSize, String sortBy, String sortOrder) {
	}

	record MerchantListQuery(Long shopId, String status, Integer page, Integer pageSize, String sortBy,
		String sortOrder) {
	}

	record OrderLineView(long productId, String productName, BigDecimal unitPrice, int quantity,
			BigDecimal subtotal) {
	}

	record OrderView(long id, String orderNumber, long userId, long shopId, String shopName,
			List<OrderLineView> lines, BigDecimal total, String status, Instant createdAt, Instant updatedAt,
			Instant cancelledAt) {
		public OrderView {
			lines = List.copyOf(lines);
		}
	}

	record OrderSummaryView(long id, String orderNumber, long shopId, String shopName, BigDecimal total,
			String status, Instant createdAt) {
	}

	record PriceChangeData(List<ShoppingService.CartItemView> currentItems) {
		public PriceChangeData {
			currentItems = List.copyOf(currentItems);
		}
	}
}
