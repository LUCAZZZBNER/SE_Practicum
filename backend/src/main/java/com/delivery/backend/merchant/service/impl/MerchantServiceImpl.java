package com.delivery.backend.merchant.service.impl;

import org.springframework.stereotype.Service;

import com.delivery.backend.merchant.service.MerchantService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class MerchantServiceImpl implements MerchantService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public MerchantView register(RegisterRequest request) {
		throw pending();
	}

	@Override
	public AuthSession login(LoginRequest request) {
		throw pending();
	}

	@Override
	public MerchantView getCurrent(long merchantId) {
		throw pending();
	}

	@Override
	public MerchantView updateCurrent(long merchantId, UpdateRequest request) {
		throw pending();
	}

	@Override
	public MerchantSnapshot requireActive(long merchantId) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
