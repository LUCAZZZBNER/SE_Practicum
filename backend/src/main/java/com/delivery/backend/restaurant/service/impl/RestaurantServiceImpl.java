package com.delivery.backend.restaurant.service.impl;

import org.springframework.stereotype.Service;

import com.delivery.backend.common.PageResult;
import com.delivery.backend.restaurant.service.RestaurantService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class RestaurantServiceImpl implements RestaurantService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public ShopView create(long merchantId, CreateRequest request) {
		throw pending();
	}

	@Override
	public PageResult<ShopView> list(ListQuery query) {
		throw pending();
	}

	@Override
	public ShopView get(long shopId) {
		throw pending();
	}

	@Override
	public ShopView update(long merchantId, long shopId, UpdateRequest request) {
		throw pending();
	}

	@Override
	public ShopSnapshot requireOrderable(long shopId) {
		throw pending();
	}

	@Override
	public ShopSnapshot requireOwned(long merchantId, long shopId) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
