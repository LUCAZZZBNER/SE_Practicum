package com.delivery.backend.shopping;

import org.springframework.stereotype.Service;

/** Entry point for cart operations. Persistence and HTTP adapters can be added behind this facade. */
@Service
public class ShoppingModule {
	public CartItem changeQuantity(CartItem cartItem, int quantity) {
		if (cartItem == null) {
			throw new IllegalArgumentException("cartItem must not be null");
		}
		return new CartItem(cartItem.id(), cartItem.userId(), cartItem.itemId(), quantity);
	}
}
