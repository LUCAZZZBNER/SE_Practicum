package com.delivery.backend.user.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.security.JwtTokenService;
import com.delivery.backend.security.Role;
import com.delivery.backend.user.dao.UserDao;
import com.delivery.backend.user.entity.UserEntity;
import com.delivery.backend.user.service.UserService;

/** User registration, authentication, profile, and active-account behavior. */
@Service
public class UserServiceImpl implements UserService {

	private static final String ACTIVE = "ACTIVE";
	private final UserDao userDao;
	private final JwtTokenService jwtTokenService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserServiceImpl(UserDao userDao, JwtTokenService jwtTokenService) {
		this.userDao = userDao;
		this.jwtTokenService = jwtTokenService;
	}

	@Override
	@Transactional
	public UserView register(RegisterRequest request) {
		if (!request.password().equals(request.passwordConfirm())) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		String account = normalizeRequired(request.account());
		if (userDao.findByAccount(account) != null) {
			throw new BusinessException(ApiError.ACCOUNT_EXISTS);
		}

		UserEntity user = new UserEntity();
		user.setAccount(account);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setNickname(normalizeRequired(request.nickname()));
		user.setPhone(normalizeNullable(request.phone()));
		user.setStatus(ACTIVE);
		try {
			userDao.insert(user);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.ACCOUNT_EXISTS);
		}
		return toView(requireById(user.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public AuthSession login(LoginRequest request) {
		UserEntity user = userDao.findByAccount(normalizeRequired(request.account()));
		if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(ApiError.BAD_CREDENTIALS);
		}
		if (!ACTIVE.equals(user.getStatus())) {
			throw new BusinessException(ApiError.ACCOUNT_DISABLED);
		}
		JwtTokenService.TokenSession token = jwtTokenService.issue(user.getId(), Role.USER);
		return new AuthSession(token.accessToken(), token.tokenType(), token.expiresIn(), toView(user),
				List.of(Role.USER.name()));
	}

	@Override
	@Transactional(readOnly = true)
	public UserView getCurrent(long userId) {
		return toView(requireById(userId));
	}

	@Override
	@Transactional
	public UserView updateCurrent(long userId, UpdateRequest request) {
		if (!request.isUpdateSpecified()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		requireById(userId);
		String nickname = request.isNicknameSpecified() ? normalizeRequired(request.nickname()) : null;
		String phone = request.isPhoneSpecified() ? normalizeNullable(request.phone()) : null;
		userDao.updateProfile(userId, request.isNicknameSpecified(), nickname,
				request.isPhoneSpecified(), phone);
		return toView(requireById(userId));
	}

	@Override
	@Transactional(readOnly = true)
	public UserSnapshot requireActive(long userId) {
		UserEntity user = requireById(userId);
		if (!ACTIVE.equals(user.getStatus())) {
			throw new BusinessException(ApiError.ACCOUNT_DISABLED);
		}
		return new UserSnapshot(user.getId(), user.getStatus());
	}

	@Override
	@Transactional
	public UserSnapshot requireActiveForUpdate(long userId) {
		UserEntity user = userDao.findByIdForUpdate(userId);
		if (user == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		if (!ACTIVE.equals(user.getStatus())) {
			throw new BusinessException(ApiError.ACCOUNT_DISABLED);
		}
		return new UserSnapshot(user.getId(), user.getStatus());
	}

	private UserEntity requireById(long userId) {
		UserEntity user = userDao.findById(userId);
		if (user == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return user;
	}

	private static UserView toView(UserEntity user) {
		return new UserView(user.getId(), user.getAccount(), user.getNickname(), user.getPhone(),
				user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
	}

	private static String normalizeRequired(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return value.trim();
	}

	private static String normalizeNullable(String value) {
		if (value == null) {
			return null;
		}
		if (value.isBlank()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return value.trim();
	}
}
