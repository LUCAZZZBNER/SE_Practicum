package com.delivery.backend.user.service.impl;

import org.springframework.stereotype.Service;

import com.delivery.backend.user.service.UserService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class UserServiceImpl implements UserService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public UserView register(RegisterRequest request) {
		throw pending();
	}

	@Override
	public AuthSession login(LoginRequest request) {
		throw pending();
	}

	@Override
	public UserView getCurrent(long userId) {
		throw pending();
	}

	@Override
	public UserView updateCurrent(long userId, UpdateRequest request) {
		throw pending();
	}

	@Override
	public UserSnapshot requireActive(long userId) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
