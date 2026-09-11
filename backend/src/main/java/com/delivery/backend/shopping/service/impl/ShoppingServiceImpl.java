package com.delivery.backend.shopping.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.item.dao.SkuDao;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.dao.ShoppingDao;
import com.delivery.backend.shopping.entity.CartItemEntity;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

/** Owned-cart mutation, current-price display, and checkout-loading behavior. */
@Service
public class ShoppingServiceImpl implements ShoppingService {

	private static final String OPEN = "OPEN";
	private static final String ON_SALE = "ON_SALE";
	private final ShoppingDao shoppingDao;
	private final UserService userService;
	private final ItemService itemService;
	private final RestaurantService restaurantService;
	private final SkuDao skuDao;

	public ShoppingServiceImpl(ShoppingDao shoppingDao, UserService userService,
			ItemService itemService, RestaurantService restaurantService, SkuDao skuDao) {
		this.shoppingDao = shoppingDao;
		this.userService = userService;
		this.itemService = itemService;
		this.restaurantService = restaurantService;
		this.skuDao = skuDao;
	}

	@Override
	@Transactional
	public AddResult add(long userId, AddRequest request) {
		userService.requireActive(userId);
		if (request.quantity() <= 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		long skuId=request.skuId();
		if(skuId<=0) throw new BusinessException(ApiError.VALIDATION_ERROR);
		var sku=skuDao.findById(skuId);
		if (sku == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		ItemService.ProductView product = requireCartProduct(sku.getProductId());
		restaurantService.requireOrderable(product.shopId());
		if (!ON_SALE.equals(product.status())) throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
		if (!ON_SALE.equals(sku.getStatus())) throw new BusinessException(ApiError.SKU_OFF_SALE);
		CartItemEntity existing = shoppingDao.findByUserAndSku(userId, skuId);
		if (existing != null) {
			return merge(userId, existing, request.quantity(), sku.getStock());
		}
		ensureStock(request.quantity(), sku.getStock());

		CartItemEntity item = new CartItemEntity();
		item.setUserId(userId);
		item.setProductId(product.id());
		item.setSkuId(sku.getId());
		item.setQuantity(request.quantity());
		try {
			shoppingDao.insert(item);
		} catch (DuplicateKeyException exception) {
			CartItemEntity concurrent = shoppingDao.findByUserAndSku(userId, skuId);
			if (concurrent == null) {
				throw exception;
			}
			return merge(userId, concurrent, request.quantity(), sku.getStock());
		}
		return new AddResult(true, toView(requireOwned(userId, item.getId())));
	}

	@Override
	@Transactional(readOnly = true)
	public CartView getCart(long userId) {
		userService.requireActive(userId);
		List<CartItemView> items = shoppingDao.listByUser(userId).stream()
				.map(ShoppingServiceImpl::toView)
				.toList();
		BigDecimal total = items.stream().map(CartItemView::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new CartView(items, total);
	}

	@Override
	@Transactional
	public CartItemView changeQuantity(long userId, long cartItemId, int quantity) {
		userService.requireActive(userId);
		if (quantity <= 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		CartItemEntity item = requireOwned(userId, cartItemId);
		ItemService.ProductView product = requireCartProduct(item.getProductId());
		restaurantService.requireOrderable(product.shopId());
		var sku = item.getSkuId() == null ? null : skuDao.findById(item.getSkuId());
		if (sku == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		if (!ON_SALE.equals(product.status())) throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
		if (!ON_SALE.equals(sku.getStatus())) throw new BusinessException(ApiError.SKU_OFF_SALE);
		ensureStock(quantity, sku.getStock());
		if (shoppingDao.updateQuantity(userId, cartItemId, quantity) != 1) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return toView(requireOwned(userId, cartItemId));
	}

	@Override
	@Transactional
	public DeleteResult remove(long userId, long cartItemId) {
		userService.requireActive(userId);
		if (shoppingDao.deleteOwned(userId, cartItemId) != 1) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return new DeleteResult(cartItemId, true);
	}

	@Override
	@Transactional
	public List<CheckoutItem> loadForCheckout(long userId, List<Long> cartItemIds) {
		userService.requireActive(userId);
		List<Long> ids = normalizeIds(cartItemIds);
		List<CartItemEntity> items = shoppingDao.listSelected(userId, ids);
		if (items.size() != ids.size()) {
			throw new BusinessException(ApiError.CART_EMPTY);
		}
		return items.stream().map(item -> {
			ensureAvailable(item);
			return new CheckoutItem(item.getId(), item.getProductId(), item.getShopId(), item.getQuantity(),
					item.getSkuVersion() == null ? item.getProductVersion() : item.getSkuVersion(), item.getSkuId() == null ? 0 : item.getSkuId(),
					item.getSkuVersion() == null ? item.getProductVersion() : item.getSkuVersion());
		}).toList();
	}

	@Override
	@Transactional
	public void removeAfterCheckout(long userId, List<Long> cartItemIds) {
		userService.requireActive(userId);
		List<Long> ids = normalizeIds(cartItemIds);
		shoppingDao.deleteSelected(userId, ids);
	}

	private AddResult merge(long userId, CartItemEntity existing, int addition, int productStock) {
		if (shoppingDao.incrementQuantity(userId, existing.getId(), addition, productStock) != 1) {
			CartItemEntity current = shoppingDao.findOwnedById(userId, existing.getId());
			if (current == null) {
				throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
			}
			throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
		}
		return new AddResult(false, toView(requireOwned(userId, existing.getId())));
	}

	private CartItemEntity requireOwned(long userId, long cartItemId) {
		CartItemEntity item = shoppingDao.findOwnedById(userId, cartItemId);
		if (item == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return item;
	}

	private static List<Long> normalizeIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			throw new BusinessException(ApiError.CART_EMPTY);
		}
		LinkedHashSet<Long> unique = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id == null || id <= 0 || !unique.add(id)) {
				throw new BusinessException(ApiError.VALIDATION_ERROR);
			}
		}
		return List.copyOf(unique);
	}

	private static CartItemView toView(CartItemEntity item) {
		CartProductView product = new CartProductView(item.getProductId(), item.getShopId(),
				item.getProductName(), item.getProductPrice(), item.getProductStock(),
				item.getProductStatus(), item.getProductVersion(), item.getProductImageUrl());
		BigDecimal unit=item.getSkuPrice()==null?item.getProductPrice():item.getSkuPrice();
		BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(item.getQuantity()));
		String unavailableReason = unavailableReason(item);
		CartSkuView sku=item.getSkuId()==null?null:new CartSkuView(item.getSkuId(),item.getSkuName(),unit,item.getSkuStock(),item.getSkuStatus(),item.getSkuVersion());
		return new CartItemView(item.getId(), product, item.getQuantity(), subtotal,
				unavailableReason == null, unavailableReason, item.getCreatedAt(), item.getUpdatedAt(),sku);
	}

	private static void ensureAvailable(CartItemEntity item) {
		if (!OPEN.equals(item.getShopStatus())) {
			throw new BusinessException(ApiError.SHOP_NOT_OPEN);
		}
		if (!ON_SALE.equals(item.getProductStatus())) {
			throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
		}
		if (!ON_SALE.equals(item.getSkuStatus())) throw new BusinessException(ApiError.SKU_OFF_SALE);
		ensureStock(item.getQuantity(), item.getSkuStock() == null ? item.getProductStock() : item.getSkuStock());
	}

	private static String unavailableReason(CartItemEntity item) {
		if (!OPEN.equals(item.getShopStatus())) {
			return ApiError.SHOP_NOT_OPEN.name();
		}
		if (!ON_SALE.equals(item.getProductStatus())) {
			return ApiError.PRODUCT_OFF_SALE.name();
		}
		if (!ON_SALE.equals(item.getSkuStatus())) return ApiError.SKU_OFF_SALE.name();
		if (item.getQuantity() > (item.getSkuStock() == null ? item.getProductStock() : item.getSkuStock())) {
			return ApiError.INSUFFICIENT_STOCK.name();
		}
		return null;
	}

	private static void ensureStock(int quantity, int stock) {
		if (quantity > stock) {
			throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
		}
	}

	private ItemService.ProductView requireCartProduct(long productId) {
		try {
			return itemService.getProduct(productId, false, null);
		} catch (BusinessException exception) {
			if (exception.error() == ApiError.RESOURCE_NOT_FOUND) throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
			throw exception;
		}
	}
}
