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

	public ShoppingServiceImpl(ShoppingDao shoppingDao, UserService userService,
			ItemService itemService, RestaurantService restaurantService) {
		this.shoppingDao = shoppingDao;
		this.userService = userService;
		this.itemService = itemService;
		this.restaurantService = restaurantService;
	}

	@Override
	@Transactional
	public AddResult add(long userId, AddRequest request) {
		userService.requireActive(userId);
		if (request.quantity() <= 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		ItemService.ProductView product = itemService.getProduct(request.productId(), false, null);
		restaurantService.requireOrderable(product.shopId());
		CartItemEntity existing = shoppingDao.findByUserAndProduct(userId, request.productId());
		if (existing != null) {
			return merge(userId, existing, request.quantity(), product.stock());
		}
		ensureStock(request.quantity(), product.stock());

		CartItemEntity item = new CartItemEntity();
		item.setUserId(userId);
		item.setProductId(request.productId());
		item.setQuantity(request.quantity());
		try {
			shoppingDao.insert(item);
		} catch (DuplicateKeyException exception) {
			CartItemEntity concurrent = shoppingDao.findByUserAndProduct(userId, request.productId());
			if (concurrent == null) {
				throw exception;
			}
			return merge(userId, concurrent, request.quantity(), product.stock());
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
		ItemService.ProductView product = itemService.getProduct(item.getProductId(), false, null);
		restaurantService.requireOrderable(product.shopId());
		ensureStock(quantity, product.stock());
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
			return new CheckoutItem(item.getId(), item.getProductId(), item.getShopId(),
					item.getQuantity(), item.getProductVersion());
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
				item.getProductStatus(), item.getProductVersion());
		BigDecimal subtotal = item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		String unavailableReason = unavailableReason(item);
		return new CartItemView(item.getId(), product, item.getQuantity(), subtotal,
				unavailableReason == null, unavailableReason, item.getCreatedAt(), item.getUpdatedAt());
	}

	private static void ensureAvailable(CartItemEntity item) {
		if (!OPEN.equals(item.getShopStatus())) {
			throw new BusinessException(ApiError.SHOP_NOT_OPEN);
		}
		if (!ON_SALE.equals(item.getProductStatus())) {
			throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
		}
		ensureStock(item.getQuantity(), item.getProductStock());
	}

	private static String unavailableReason(CartItemEntity item) {
		if (!OPEN.equals(item.getShopStatus())) {
			return ApiError.SHOP_NOT_OPEN.name();
		}
		if (!ON_SALE.equals(item.getProductStatus())) {
			return ApiError.PRODUCT_OFF_SALE.name();
		}
		if (item.getQuantity() > item.getProductStock()) {
			return ApiError.INSUFFICIENT_STOCK.name();
		}
		return null;
	}

	private static void ensureStock(int quantity, int stock) {
		if (quantity > stock) {
			throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
		}
	}
}
