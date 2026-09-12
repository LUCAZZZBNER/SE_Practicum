package com.delivery.backend.item.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.item.dao.ItemDao;
import com.delivery.backend.item.dao.SkuDao;
import com.delivery.backend.image.service.ImageService;
import com.delivery.backend.item.entity.CategoryEntity;
import com.delivery.backend.item.entity.ProductEntity;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.item.service.SkuService;
import com.delivery.backend.restaurant.service.RestaurantService;

/** Category, product, optimistic-update, and atomic-stock behavior. */
@Service
public class ItemServiceImpl implements ItemService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final String ON_SALE = "ON_SALE";
	private static final String OFF_SALE = "OFF_SALE";
	private final ItemDao itemDao;
	private final RestaurantService restaurantService;
	private final SkuDao skuDao;
	private final ImageService imageService;

	public ItemServiceImpl(ItemDao itemDao, RestaurantService restaurantService) {
		this(itemDao, restaurantService, null, null);
	}
	public ItemServiceImpl(ItemDao itemDao, RestaurantService restaurantService, SkuDao skuDao) {
		this(itemDao, restaurantService, skuDao, null);
	}
	@Autowired
	public ItemServiceImpl(ItemDao itemDao, RestaurantService restaurantService, SkuDao skuDao, ImageService imageService) {
		this.itemDao = itemDao;
		this.restaurantService = restaurantService;
		this.skuDao = skuDao;
		this.imageService = imageService;
	}

	@Override
	@Transactional
	public CategoryView createCategory(long merchantId, long shopId, CreateCategoryRequest request) {
		restaurantService.requireOwned(merchantId, shopId);
		String name = normalizeRequired(request.name());
		if (itemDao.findCategoryByShopAndName(shopId, name) != null) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
		if (sortOrder < 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}

		CategoryEntity category = new CategoryEntity();
		category.setShopId(shopId);
		category.setName(name);
		category.setSortOrder(sortOrder);
		try {
			itemDao.insertCategory(category);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		return toCategoryView(requireCategory(category.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryView> listCategories(long shopId) {
		restaurantService.get(shopId);
		return itemDao.listCategories(shopId).stream().map(ItemServiceImpl::toCategoryView).toList();
	}

	@Override
	@Transactional
	public CategoryView updateCategory(long merchantId, long categoryId, UpdateCategoryRequest request) {
		if (!request.isUpdateSpecified()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		CategoryEntity category = requireCategory(categoryId);
		restaurantService.requireOwned(merchantId, category.getShopId());
		String name = request.isNameSpecified() ? normalizeRequired(request.name()) : null;
		if (name != null) {
			CategoryEntity duplicate = itemDao.findCategoryByShopAndName(category.getShopId(), name);
			if (duplicate != null && !duplicate.getId().equals(category.getId())) {
				throw new BusinessException(ApiError.RESOURCE_CONFLICT);
			}
		}
		Integer sortOrder = request.isSortOrderSpecified() ? request.sortOrder() : null;
		if (sortOrder != null && sortOrder < 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		try {
			itemDao.updateCategory(categoryId, request.isNameSpecified(), name,
					request.isSortOrderSpecified(), sortOrder);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		return toCategoryView(requireCategory(categoryId));
	}

	@Override
	@Transactional
	public DeleteResult deleteCategory(long merchantId, long categoryId) {
		CategoryEntity category = requireCategory(categoryId);
		restaurantService.requireOwned(merchantId, category.getShopId());
		if (itemDao.countProductsByCategory(categoryId) > 0) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		if (itemDao.logicalDeleteCategory(categoryId) != 1) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return new DeleteResult(categoryId, true);
	}

	@Override
	@Transactional
	public ProductView createProduct(long merchantId, CreateProductRequest request) {
		restaurantService.requireOwned(merchantId, request.shopId());
		CategoryEntity category = requireCategory(request.categoryId());
		if (category.getShopId() != request.shopId()) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		List<SkuService.CreateRequest> skus=request.skus();
		if (skus.isEmpty()) throw new BusinessException(ApiError.VALIDATION_ERROR);
		if (request.imageId() != null && imageService != null) {
			imageService.requireOwned(merchantId, request.imageId());
		}
		BigDecimal price=request.price(); int stock=request.stock()==null?0:request.stock();
		if(price==null){price=skus.stream().map(SkuService.CreateRequest::price).min(BigDecimal::compareTo).orElseThrow();}
		if(request.stock()==null) stock=skus.stream().mapToInt(SkuService.CreateRequest::stock).sum();
		validatePrice(price); if(stock<0) throw new BusinessException(ApiError.VALIDATION_ERROR);

		ProductEntity product = new ProductEntity();
		product.setShopId(request.shopId());
		product.setCategoryId(request.categoryId());
		product.setName(normalizeRequired(request.name()));
		product.setDescription(request.description());
		product.setPrice(price);
		product.setStock(stock);
		product.setStatus(OFF_SALE);
		product.setVersion(1L);
		product.setImageId(request.imageId());
		itemDao.insertProduct(product);
		if(skuDao!=null) for(SkuService.CreateRequest requestSku:skus){var sku=new com.delivery.backend.item.entity.SkuEntity();sku.setProductId(product.getId());sku.setName(requestSku.name().trim());sku.setPrice(requestSku.price());sku.setStock(requestSku.stock());sku.setStatus(OFF_SALE);sku.setVersion(1L);skuDao.insert(sku);}
		return toProductView(requireProduct(product.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<ProductView> listProducts(long shopId, ProductQuery query) {
		restaurantService.get(shopId);
		boolean includeOffSale = Boolean.TRUE.equals(query.includeOffSale());
		if (includeOffSale) {
			if (query.merchantId() == null) {
				throw new BusinessException(ApiError.FORBIDDEN);
			}
			restaurantService.requireOwned(query.merchantId(), shopId);
		}
		if (query.categoryId() != null) {
			CategoryEntity category = requireCategory(query.categoryId());
			if (category.getShopId() != shopId) {
				throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
			}
		}
		int page = defaultPage(query.page());
		int pageSize = defaultPageSize(query.pageSize());
		String sortBy = validateSortBy(query.sortBy());
		String sortOrder = validateSortOrder(query.sortOrder());
		String keyword = normalizeSearch(query.keyword());
		long offset = (long) (page - 1) * pageSize;
		List<ProductView> items = itemDao.listProducts(shopId, query.categoryId(), keyword, includeOffSale,
				sortBy, sortOrder, pageSize, offset).stream().map(product -> toProductView(product, includeOffSale)).toList();
		long total = itemDao.countProducts(shopId, query.categoryId(), keyword, includeOffSale);
		int totalPages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
		return new PageResult<>(items, page, pageSize, total, totalPages);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductView getProduct(long productId, boolean includeOffSale, Long merchantId) {
		ProductEntity product = requireProduct(productId);
		if (includeOffSale) {
			if (merchantId == null) {
				throw new BusinessException(ApiError.FORBIDDEN);
			}
			restaurantService.requireOwned(merchantId, product.getShopId());
		} else if (!ON_SALE.equals(product.getStatus())) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		ProductView view = toProductView(product, includeOffSale);
		if (!includeOffSale && view.skus().stream().noneMatch(sku -> ON_SALE.equals(sku.status()) && sku.stock() > 0)) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return view;
	}

	@Override
	@Transactional
	public ProductView updateProduct(long merchantId, long productId, UpdateProductRequest request) {
		if (!request.isUpdateSpecified()) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		ProductEntity product = requireProduct(productId);
		restaurantService.requireOwned(merchantId, product.getShopId());
		if (request.isCategoryIdSpecified()) {
			CategoryEntity category = requireCategory(request.categoryId());
			if (category.getShopId() != product.getShopId()) {
				throw new BusinessException(ApiError.RESOURCE_CONFLICT);
			}
		}
		String name = request.isNameSpecified() ? normalizeRequired(request.name()) : null;
		if (request.isPriceSpecified()) {
			validatePrice(request.price());
		}
		if (request.isStockSpecified() && (request.stock() == null || request.stock() < 0)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		String status = request.isStatusSpecified() ? validateProductStatus(request.status()) : null;
		Long imageId = request.isImageIdSpecified() ? request.imageId() : product.getImageId();
		if (imageId != null && imageService != null) imageService.requireOwned(merchantId, imageId);
		if (ON_SALE.equals(status) && (imageId == null || skuDao == null || skuDao.listByProduct(productId).isEmpty())) {
			throw new BusinessException(ApiError.IMAGE_REQUIRED);
		}
		long expectedVersion = request.version() == null ? product.getVersion() : request.version();
		int updated = itemDao.updateProduct(productId, expectedVersion,
				request.isCategoryIdSpecified(), request.categoryId(),
				request.isNameSpecified(), name,
				request.isDescriptionSpecified(), request.description(),
				request.isPriceSpecified(), request.price(),
				request.isStockSpecified(), request.stock(),
				request.isStatusSpecified(), status, request.isImageIdSpecified(), request.imageId());
		if (updated != 1) {
			throw new BusinessException(ApiError.RESOURCE_CONFLICT);
		}
		return toProductView(requireProduct(productId));
	}

	@Override
	@Transactional
	public List<ProductSnapshot> reserveForOrder(List<ReservationRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			throw new BusinessException(ApiError.CART_EMPTY);
		}
		return requests.stream()
				.sorted(Comparator.comparingLong(ReservationRequest::productId))
				.map(this::reserveOne)
				.toList();
	}

	@Override
	@Transactional
	public void restoreStock(List<StockRestore> restorations) {
		if (restorations == null) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		for (StockRestore restoration : restorations) {
			if (restoration.quantity() <= 0) {
				throw new BusinessException(ApiError.VALIDATION_ERROR);
			}
			int restored = restoration.skuId() > 0 ? skuDao.restoreStock(restoration.skuId(), restoration.quantity())
					: itemDao.restoreStock(restoration.productId(), restoration.quantity());
			if (restored != 1) {
				throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
			}
		}
	}

	private ProductSnapshot reserveOne(ReservationRequest request) {
		if (request.quantity() <= 0 || request.expectedVersion() <= 0) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		ProductEntity product = requireProduct(request.productId());
		if (request.skuId() > 0) {
			var sku = skuDao.findById(request.skuId());
			if (sku == null || !sku.getProductId().equals(product.getId())) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
			if (!ON_SALE.equals(product.getStatus())) throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
			if (!ON_SALE.equals(sku.getStatus())) throw new BusinessException(ApiError.SKU_OFF_SALE);
			if (sku.getVersion() != request.expectedVersion()) throw new BusinessException(ApiError.PRICE_CHANGED);
			if (sku.getStock() < request.quantity()) throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
			if (skuDao.reserveStock(sku.getId(), request.expectedVersion(), request.quantity()) != 1) throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
			return new ProductSnapshot(product.getId(), product.getShopId(), product.getName(), sku.getPrice(), request.quantity(), sku.getVersion(), sku.getId(), sku.getName(), product.getImageUrl());
		}
		if (!ON_SALE.equals(product.getStatus())) {
			throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
		}
		if (product.getVersion() != request.expectedVersion()) {
			throw new BusinessException(ApiError.PRICE_CHANGED);
		}
		if (product.getStock() < request.quantity()) {
			throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
		}
		if (itemDao.reserveStock(product.getId(), request.expectedVersion(), request.quantity()) != 1) {
			ProductEntity current = requireProduct(product.getId());
			if (!ON_SALE.equals(current.getStatus())) {
				throw new BusinessException(ApiError.PRODUCT_OFF_SALE);
			}
			if (current.getVersion() != request.expectedVersion()) {
				throw new BusinessException(ApiError.PRICE_CHANGED);
			}
			throw new BusinessException(ApiError.INSUFFICIENT_STOCK);
		}
		return new ProductSnapshot(product.getId(), product.getShopId(), product.getName(),
				product.getPrice(), request.quantity(), product.getVersion(), 0, null, product.getImageUrl());
	}

	private CategoryEntity requireCategory(long categoryId) {
		CategoryEntity category = itemDao.findCategoryById(categoryId);
		if (category == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return category;
	}

	private ProductEntity requireProduct(long productId) {
		ProductEntity product = itemDao.findProductById(productId);
		if (product == null) {
			throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
		}
		return product;
	}

	private static CategoryView toCategoryView(CategoryEntity category) {
		return new CategoryView(category.getId(), category.getShopId(), category.getName(),
				category.getSortOrder(), category.getCreatedAt(), category.getUpdatedAt());
	}

	private ProductView toProductView(ProductEntity product) { return toProductView(product, true); }

	private ProductView toProductView(ProductEntity product, boolean includeOffSale) {
		List<SkuService.SkuView> skus=skuDao==null?List.of():skuDao.listByProduct(product.getId()).stream().filter(s -> includeOffSale || ON_SALE.equals(s.getStatus())).map(s->new SkuService.SkuView(s.getId(),s.getProductId(),s.getName(),s.getPrice(),s.getStock(),s.getStatus(),s.getVersion(),s.getCreatedAt(),s.getUpdatedAt())).toList();
		ImageView image = product.getImageId() == null ? null : new ImageView(product.getImageId(), product.getImageUrl());
		if (product.getImageId() != null && imageService != null) {
			var asset = imageService.require(product.getImageId());
			image = new ImageView(asset.id(), asset.url(), asset.contentType(), asset.size(), asset.createdAt());
		}
		BigDecimal minPrice = skus.stream().map(SkuService.SkuView::price).min(BigDecimal::compareTo).orElse(product.getPrice());
		int stock = skus.stream().mapToInt(SkuService.SkuView::stock).sum();
		return new ProductView(product.getId(), product.getShopId(), product.getCategoryId(),
				product.getName(), product.getDescription(), minPrice, stock,
				product.getStatus(), product.getVersion(), product.getCreatedAt(), product.getUpdatedAt(),image,skus);
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

	private static String validateSortBy(String sortBy) {
		if (sortBy == null) {
			return "createdAt";
		}
		if (!"name".equals(sortBy) && !"price".equals(sortBy) && !"createdAt".equals(sortBy)) {
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

	private static String validateProductStatus(String status) {
		if (!ON_SALE.equals(status) && !OFF_SALE.equals(status)) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
		return status;
	}

	private static void validatePrice(BigDecimal price) {
		if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 2) {
			throw new BusinessException(ApiError.VALIDATION_ERROR);
		}
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
