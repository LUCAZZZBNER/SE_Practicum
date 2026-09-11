package com.delivery.backend.address.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.address.dao.UserAddressDao;
import com.delivery.backend.address.entity.UserAddressEntity;
import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.user.service.UserService;

@Service
public class UserAddressServiceImpl implements UserAddressService {
	private final UserAddressDao dao;
	private final UserService userService;
	public UserAddressServiceImpl(UserAddressDao dao, UserService userService) { this.dao = dao; this.userService = userService; }

	@Override @Transactional
	public AddressView create(long userId, CreateRequest request) {
		userService.requireActiveForUpdate(userId);
		validate(request.recipient(), request.phone(), request.region(), request.detail());
		if (Boolean.TRUE.equals(request.isDefault())) dao.clearDefaults(userId);
		UserAddressEntity entity = new UserAddressEntity();
		entity.setUserId(userId); entity.setRecipient(request.recipient().trim()); entity.setPhone(request.phone().trim());
		entity.setRegion(request.region().trim()); entity.setDetail(request.detail().trim());
		entity.setDefaultAddress(Boolean.TRUE.equals(request.isDefault()));
		dao.insert(entity);
		return toView(owned(userId, entity.getId()));
	}

	@Override @Transactional(readOnly = true)
	public List<AddressView> list(long userId) { userService.requireActive(userId); return dao.list(userId).stream().map(UserAddressServiceImpl::toView).toList(); }

	@Override @Transactional
	public AddressView update(long userId, long addressId, UpdateRequest request) {
		userService.requireActiveForUpdate(userId); owned(userId, addressId);
		if (!request.isUpdateSpecified()) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.phoneSpecified() && (request.phone() == null || !request.phone().matches("1[0-9]{10}"))) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.recipientSpecified() && (request.recipient() == null || request.recipient().isBlank())) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.regionSpecified() && (request.region() == null || request.region().isBlank())) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.detailSpecified() && (request.detail() == null || request.detail().isBlank())) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.defaultSpecified() && Boolean.TRUE.equals(request.isDefault())) dao.clearDefaults(userId);
		dao.update(addressId, userId, request.recipientSpecified(), trim(request.recipient()), request.phoneSpecified(), trim(request.phone()),
				request.regionSpecified(), trim(request.region()), request.detailSpecified(), trim(request.detail()), request.defaultSpecified(), request.isDefault());
		return toView(owned(userId, addressId));
	}

	@Override @Transactional
	public DeleteResult remove(long userId, long addressId) { userService.requireActive(userId); owned(userId, addressId); if (dao.softDelete(userId, addressId) != 1) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND); return new DeleteResult(addressId, true); }

	@Override @Transactional(readOnly = true)
	public AddressView requireOwned(long userId, long addressId) { return toView(owned(userId, addressId)); }

	private UserAddressEntity owned(long userId, long addressId) { UserAddressEntity entity = dao.findOwned(userId, addressId); if (entity == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND); return entity; }
	private static void validate(String recipient, String phone, String region, String detail) { if (recipient == null || recipient.isBlank() || phone == null || !phone.matches("1[0-9]{10}") || region == null || region.isBlank() || detail == null || detail.isBlank()) throw new BusinessException(ApiError.VALIDATION_ERROR); }
	private static String trim(String value) { return value == null ? null : value.trim(); }
	private static AddressView toView(UserAddressEntity e) { return new AddressView(e.getId(), e.getUserId(), e.getRecipient(), e.getPhone(), e.getRegion(), e.getDetail(), Boolean.TRUE.equals(e.getDefaultAddress()), e.getCreatedAt(), e.getUpdatedAt()); }
}
