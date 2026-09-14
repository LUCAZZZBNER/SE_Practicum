package com.delivery.backend.merchant.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.merchant.dao.MerchantDao;
import com.delivery.backend.merchant.entity.MerchantEntity;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.security.JwtTokenService;
import com.delivery.backend.security.Role;

/** Independent merchant registration, authentication, profile, and status behavior. */
@Service
public class MerchantServiceImpl implements MerchantService {

	private static final String ACTIVE = "ACTIVE";
	private final MerchantDao merchantDao;
	private final JwtTokenService jwtTokenService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public MerchantServiceImpl(MerchantDao merchantDao, JwtTokenService jwtTokenService) {
		this.merchantDao = merchantDao;
		this.jwtTokenService = jwtTokenService;
	}

	@Override
	@Transactional
	public MerchantView register(RegisterRequest request) {
		if (request == null || request.password() == null) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		if (request.passwordConfirm() != null
				&& !request.password().equals(request.passwordConfirm())) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		String account = normalizeRequired(request.account());
		if (merchantDao.findByAccount(account) != null) {
			throw new BusinessException(ApiError.MERCHANT_ACCOUNT_EXISTS);
		}

		MerchantEntity merchant = new MerchantEntity();
		merchant.setAccount(account);
		merchant.setPasswordHash(passwordEncoder.encode(request.password()));
		merchant.setName(normalizeRequired(request.name()));
		merchant.setPhone(normalizeRequired(request.phone()));
		merchant.setStatus(ACTIVE);
		try {
			merchantDao.insert(merchant);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.MERCHANT_ACCOUNT_EXISTS);
		}
		return toView(requireById(merchant.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public AuthSession login(LoginRequest request) {
		MerchantEntity merchant = merchantDao.findByAccount(normalizeRequired(request.account()));
		if (merchant == null
				|| !passwordEncoder.matches(request.password(), merchant.getPasswordHash())) {
			throw new BusinessException(ApiError.BAD_CREDENTIALS);
		}
		ensureActive(merchant);
		JwtTokenService.TokenSession token = jwtTokenService.issue(merchant.getId(), Role.MERCHANT);
		return new AuthSession(token.accessToken(), token.tokenType(), token.expiresIn(), toView(merchant),
				List.of(Role.MERCHANT.name()));
	}

	@Override
	@Transactional(readOnly = true)
	public MerchantView getCurrent(long merchantId) {
		return toView(requireById(merchantId));
	}

	@Override
	@Transactional
	public MerchantView updateCurrent(long merchantId, UpdateRequest request) {
		if (!request.isUpdateSpecified()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		ensureActive(requireById(merchantId));
		String name = request.isNameSpecified() ? normalizeRequired(request.name()) : null;
		String phone = request.isPhoneSpecified() ? normalizeRequired(request.phone()) : null;
		merchantDao.updateProfile(merchantId, request.isNameSpecified(), name,
				request.isPhoneSpecified(), phone);
		return toView(requireById(merchantId));
	}

	@Override
	@Transactional(readOnly = true)
	public MerchantSnapshot requireActive(long merchantId) {
		MerchantEntity merchant = requireById(merchantId);
		ensureActive(merchant);
		return new MerchantSnapshot(merchant.getId(), merchant.getStatus());
	}

	private MerchantEntity requireById(long merchantId) {
		MerchantEntity merchant = merchantDao.findById(merchantId);
		if (merchant == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return merchant;
	}

	private static void ensureActive(MerchantEntity merchant) {
		if (!ACTIVE.equals(merchant.getStatus())) {
			throw new BusinessException(ApiError.MERCHANT_SUSPENDED);
		}
	}

	private static MerchantView toView(MerchantEntity merchant) {
		return new MerchantView(merchant.getId(), merchant.getAccount(), merchant.getName(),
				merchant.getPhone(), merchant.getStatus(), merchant.getCreatedAt(), merchant.getUpdatedAt());
	}

	private static String normalizeRequired(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return value.trim();
	}
}
