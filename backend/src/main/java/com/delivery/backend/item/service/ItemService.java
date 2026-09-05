package com.delivery.backend.item.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.delivery.backend.common.PatchRequest;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.common.PageResult;

/** Category and product business contract. Implementation and persistence are intentionally pending. */
public interface ItemService {

	CategoryView createCategory(long merchantId, long shopId, CreateCategoryRequest request);

	List<CategoryView> listCategories(long shopId);

	CategoryView updateCategory(long merchantId, long categoryId, UpdateCategoryRequest request);

	DeleteResult deleteCategory(long merchantId, long categoryId);

	ProductView createProduct(long merchantId, CreateProductRequest request);

	PageResult<ProductView> listProducts(long shopId, ProductQuery query);

	ProductView getProduct(long productId, boolean includeOffSale, Long merchantId);

	ProductView updateProduct(long merchantId, long productId, UpdateProductRequest request);

	List<ProductSnapshot> reserveForOrder(List<ReservationRequest> requests);

	void restoreStock(List<StockRestore> restorations);

	record CreateCategoryRequest(@NotBlank String name, @PositiveOrZero Integer sortOrder) {
	}

	final class UpdateCategoryRequest extends PatchRequest {
		@Pattern(regexp = "(?s).*\\S.*")
		private String name;
		@PositiveOrZero
		private Integer sortOrder;

		public UpdateCategoryRequest() {
		}

		@Override
		@JsonIgnore
		@AssertTrue
		public boolean isUpdateSpecified() {
			return super.isUpdateSpecified();
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			markUpdateSpecified();
		}

		public Integer sortOrder() {
			return sortOrder;
		}

		public void setSortOrder(Integer sortOrder) {
			this.sortOrder = sortOrder;
			markUpdateSpecified();
		}
	}

	record CreateProductRequest(@Positive long shopId, @Positive long categoryId, @NotBlank String name, String description,
			@NotNull @DecimalMin("0.01") @Digits(integer = 1000, fraction = 2) BigDecimal price,
			@PositiveOrZero int stock) {
	}

	final class UpdateProductRequest extends PatchRequest {
		@Positive
		private Long categoryId;
		@Pattern(regexp = "(?s).*\\S.*")
		private String name;
		private String description;
		@DecimalMin("0.01")
		@Digits(integer = 1000, fraction = 2)
		private BigDecimal price;
		@PositiveOrZero
		private Integer stock;
		@Pattern(regexp = "ON_SALE|OFF_SALE")
		private String status;
		@Positive
		private Long version;

		public UpdateProductRequest() {
		}

		@Override
		@JsonIgnore
		@AssertTrue
		public boolean isUpdateSpecified() {
			return super.isUpdateSpecified();
		}

		public Long categoryId() {
			return categoryId;
		}

		public void setCategoryId(Long categoryId) {
			this.categoryId = categoryId;
			markUpdateSpecified();
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			markUpdateSpecified();
		}

		public String description() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
			markUpdateSpecified();
		}

		public BigDecimal price() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
			markUpdateSpecified();
		}

		public Integer stock() {
			return stock;
		}

		public void setStock(Integer stock) {
			this.stock = stock;
			markUpdateSpecified();
		}

		public String status() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
			markUpdateSpecified();
		}

		public Long version() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
			markUpdateSpecified();
		}
	}

	record ProductQuery(Long categoryId, String keyword, Integer page, Integer pageSize, String sortBy,
		String sortOrder, Boolean includeOffSale, Long merchantId) {
	}

	record CategoryView(long id, long shopId, String name, int sortOrder, Instant createdAt, Instant updatedAt) {
	}

	record ProductView(long id, long shopId, long categoryId, String name, String description, BigDecimal price,
			int stock, String status, long version, Instant createdAt, Instant updatedAt) {
	}

	record ReservationRequest(long productId, long expectedVersion, int quantity) {
	}

	record ProductSnapshot(long productId, long shopId, String name, BigDecimal unitPrice, int quantity,
			long version) {
	}

	record StockRestore(long productId, int quantity) {
	}
}
