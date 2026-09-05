package com.delivery.backend.item.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.item.service.ItemService;

/** Minimal injectable shell. Business behavior is implemented one test at a time. */
@Service
public class ItemServiceImpl implements ItemService {

	private static final String PENDING_MESSAGE = "Pending TDD implementation";

	@Override
	public CategoryView createCategory(long merchantId, long shopId, CreateCategoryRequest request) {
		throw pending();
	}

	@Override
	public List<CategoryView> listCategories(long shopId) {
		throw pending();
	}

	@Override
	public CategoryView updateCategory(long merchantId, long categoryId, UpdateCategoryRequest request) {
		throw pending();
	}

	@Override
	public DeleteResult deleteCategory(long merchantId, long categoryId) {
		throw pending();
	}

	@Override
	public ProductView createProduct(long merchantId, CreateProductRequest request) {
		throw pending();
	}

	@Override
	public PageResult<ProductView> listProducts(long shopId, ProductQuery query) {
		throw pending();
	}

	@Override
	public ProductView getProduct(long productId, boolean includeOffSale, Long merchantId) {
		throw pending();
	}

	@Override
	public ProductView updateProduct(long merchantId, long productId, UpdateProductRequest request) {
		throw pending();
	}

	@Override
	public List<ProductSnapshot> reserveForOrder(List<ReservationRequest> requests) {
		throw pending();
	}

	@Override
	public void restoreStock(List<StockRestore> restorations) {
		throw pending();
	}

	private UnsupportedOperationException pending() {
		return new UnsupportedOperationException(PENDING_MESSAGE);
	}
}
