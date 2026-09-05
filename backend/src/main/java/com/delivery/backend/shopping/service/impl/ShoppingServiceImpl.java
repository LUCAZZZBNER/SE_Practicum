package com.delivery.backend.shopping.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.shopping.service.ShoppingService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class ShoppingServiceImpl implements ShoppingService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public AddResult add(long userId, AddRequest request) {
		throw pending();
	}

	@Override
	public CartView getCart(long userId) {
		throw pending();
	}

	@Override
	public CartItemView changeQuantity(long userId, long cartItemId, int quantity) {
		throw pending();
	}

	@Override
	public DeleteResult remove(long userId, long cartItemId) {
		throw pending();
	}

	@Override
	public List<CheckoutItem> loadForCheckout(long userId, List<Long> cartItemIds) {
		throw pending();
	}

	@Override
	public void removeAfterCheckout(long userId, List<Long> cartItemIds) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
