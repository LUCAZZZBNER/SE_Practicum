package com.delivery.backend.order.service.impl;

import org.springframework.stereotype.Service;

import com.delivery.backend.common.PageResult;
import com.delivery.backend.order.service.OrderService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class OrderServiceImpl implements OrderService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public OrderView create(long userId, String idempotencyKey, CreateRequest request) {
		throw pending();
	}

	@Override
	public PageResult<OrderSummaryView> listMine(long userId, ListQuery query) {
		throw pending();
	}

	@Override
	public OrderView getMine(long userId, long orderId) {
		throw pending();
	}

	@Override
	public OrderView cancel(long userId, long orderId) {
		throw pending();
	}

	@Override
	public PageResult<OrderSummaryView> listMerchantOrders(long merchantId, MerchantListQuery query) {
		throw pending();
	}

	@Override
	public OrderView getMerchantOrder(long merchantId, long orderId) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
