package com.delivery.backend.shopping.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.DeleteResult;

/** Shopping-cart business contract. Implementation and persistence are intentionally pending. */
public interface ShoppingService {

	AddResult add(long userId, AddRequest request);

	CartView getCart(long userId);

	CartItemView changeQuantity(long userId, long cartItemId, int quantity);

	DeleteResult remove(long userId, long cartItemId);

	List<CheckoutItem> loadForCheckout(long userId, List<Long> cartItemIds);

	void removeAfterCheckout(long userId, List<Long> cartItemIds);

	record AddRequest(@Positive long productId, @Positive int quantity) {
	}

	record AddResult(boolean created, CartItemView item) {
	}

	record CartProductView(long id, long shopId, String name, BigDecimal price, int stock, String status,
			long version) {
	}

	record CartItemView(long id, CartProductView product, int quantity, BigDecimal subtotal, boolean available,
			String unavailableReason, Instant createdAt, Instant updatedAt) {
	}

	record CartView(List<CartItemView> items, BigDecimal total) {
		public CartView {
			items = List.copyOf(items);
		}
	}

	record CheckoutItem(long cartItemId, long productId, long shopId, int quantity, long confirmedVersion) {
	}
}
