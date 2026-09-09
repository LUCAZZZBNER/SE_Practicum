package com.delivery.backend.restaurant.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.dao.RestaurantDao;
import com.delivery.backend.restaurant.entity.ShopEntity;
import com.delivery.backend.restaurant.service.RestaurantService;

/** Shop creation, public discovery, ownership, and orderability behavior. */
@Service
public class RestaurantServiceImpl implements RestaurantService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final String DEFAULT_STATUS = "CLOSED";
	private static final String OPEN = "OPEN";
	private final RestaurantDao restaurantDao;
	private final MerchantService merchantService;

	public RestaurantServiceImpl(RestaurantDao restaurantDao, MerchantService merchantService) {
		this.restaurantDao = restaurantDao;
		this.merchantService = merchantService;
	}

	@Override
	@Transactional
	public ShopView create(long merchantId, CreateRequest request) {
		merchantService.requireActive(merchantId);
		String name = normalizeRequired(request.name());
		if (restaurantDao.findByMerchantAndName(merchantId, name) != null) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}

		ShopEntity shop = new ShopEntity();
		shop.setMerchantId(merchantId);
		shop.setName(name);
		shop.setDescription(request.description());
		shop.setStatus(DEFAULT_STATUS);
		try {
			restaurantDao.insert(shop);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		return toView(requireById(shop.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ShopView> list(ListQuery query) {
		int page = defaultPage(query.page());
		int pageSize = defaultPageSize(query.pageSize());
		boolean mine = Boolean.TRUE.equals(query.mine());
		if (mine) {
			if (query.merchantId() == null) {
				throw new BusinessException(ApiError.FORBIDDEN);
			}
			merchantService.requireActive(query.merchantId());
		}
		String keyword = normalizeSearch(query.keyword());
		String status = validateStatus(query.status());
		String sortBy = validateSortBy(query.sortBy());
		String sortOrder = validateSortOrder(query.sortOrder());
		long offset = (long) (page - 1) * pageSize;
		List<ShopView> items = restaurantDao.list(mine, query.merchantId(), keyword, status,
				sortBy, sortOrder, pageSize, offset).stream().map(RestaurantServiceImpl::toView).toList();
		long total = restaurantDao.count(mine, query.merchantId(), keyword, status);
		int totalPages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
		return new PageResult<>(items, page, pageSize, total, totalPages);
	}

	@Override
	@Transactional(readOnly = true)
	public ShopView get(long shopId) {
		return toView(requireById(shopId));
	}

	@Override
	@Transactional
	public ShopView update(long merchantId, long shopId, UpdateRequest request) {
		if (!request.isUpdateSpecified()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		ShopEntity shop = requireOwnedEntity(merchantId, shopId);
		String name = request.isNameSpecified() ? normalizeRequired(request.name()) : null;
		if (name != null) {
			ShopEntity duplicate = restaurantDao.findByMerchantAndName(merchantId, name);
			if (duplicate != null && !duplicate.getId().equals(shop.getId())) {
				throw new BusinessException(ApiError.RESOURCE_CONFLICT);
			}
		}
		String status = request.isStatusSpecified() ? validateRequiredStatus(request.status()) : null;
		try {
			restaurantDao.update(shopId, request.isNameSpecified(), name,
					request.isDescriptionSpecified(), request.description(),
					request.isStatusSpecified(), status);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		return toView(requireById(shopId));
	}

	@Override
	@Transactional(readOnly = true)
	public ShopSnapshot requireOrderable(long shopId) {
		ShopEntity shop = requireById(shopId);
		if (!OPEN.equals(shop.getStatus())) {
			throw new BusinessException(ApiError.SHOP_NOT_OPEN);
		}
		return toSnapshot(shop);
	}

	@Override
	@Transactional(readOnly = true)
	public ShopSnapshot requireOwned(long merchantId, long shopId) {
		return toSnapshot(requireOwnedEntity(merchantId, shopId));
	}

	@Override
	@Transactional(readOnly = true)
	public ShopSnapshot requireOwnedForRead(long merchantId, long shopId) {
		ShopEntity shop = requireById(shopId);
		if (shop.getMerchantId() != merchantId) {
			throw new BusinessException(ApiError.FORBIDDEN);
		}
		return toSnapshot(shop);
	}

	private ShopEntity requireOwnedEntity(long merchantId, long shopId) {
		merchantService.requireActive(merchantId);
		ShopEntity shop = requireById(shopId);
		if (shop.getMerchantId() != merchantId) {
			throw new BusinessException(ApiError.FORBIDDEN);
		}
		return shop;
	}

	private ShopEntity requireById(long shopId) {
		ShopEntity shop = restaurantDao.findById(shopId);
		if (shop == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return shop;
	}

	private static ShopView toView(ShopEntity shop) {
		return new ShopView(shop.getId(), shop.getMerchantId(), shop.getName(), shop.getDescription(),
				shop.getStatus(), shop.getCreatedAt(), shop.getUpdatedAt());
	}

	private static ShopSnapshot toSnapshot(ShopEntity shop) {
		return new ShopSnapshot(shop.getId(), shop.getMerchantId(), shop.getName(), shop.getStatus());
	}

	private static int defaultPage(Integer page) {
		int result = page == null ? DEFAULT_PAGE : page;
		if (result < 1) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return result;
	}

	private static int defaultPageSize(Integer pageSize) {
		int result = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
		if (result < 1 || result > 100) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return result;
	}

	private static String validateStatus(String status) {
		if (status == null) {
			return null;
		}
		return validateRequiredStatus(status);
	}

	private static String validateRequiredStatus(String status) {
		if (!OPEN.equals(status) && !DEFAULT_STATUS.equals(status)
				&& !"TEMPORARILY_CLOSED".equals(status)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return status;
	}

	private static String validateSortBy(String sortBy) {
		if (sortBy == null) {
			return "createdAt";
		}
		if (!"name".equals(sortBy) && !"createdAt".equals(sortBy)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return sortBy;
	}

	private static String validateSortOrder(String sortOrder) {
		if (sortOrder == null) {
			return "desc";
		}
		if (!"asc".equals(sortOrder) && !"desc".equals(sortOrder)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return sortOrder;
	}

	private static String normalizeRequired(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return value.trim();
	}

	private static String normalizeSearch(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
