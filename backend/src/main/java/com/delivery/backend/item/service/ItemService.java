package com.delivery.backend.item.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
		private boolean nameSpecified;
		private boolean sortOrderSpecified;

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
			this.nameSpecified = true;
			markUpdateSpecified();
		}

		public Integer sortOrder() {
			return sortOrder;
		}

		public void setSortOrder(Integer sortOrder) {
			this.sortOrder = sortOrder;
			this.sortOrderSpecified = true;
			markUpdateSpecified();
		}

		@JsonIgnore
		public boolean isNameSpecified() {
			return nameSpecified;
		}

		@JsonIgnore
		public boolean isSortOrderSpecified() {
			return sortOrderSpecified;
		}
	}

	record CreateProductRequest(@Positive long shopId, @Positive long categoryId, @NotBlank String name, String description,
			BigDecimal price, Integer stock, @Positive Long imageId,
			@NotEmpty @Valid List<SkuService.CreateRequest> skus) {
		public CreateProductRequest(long shopId,long categoryId,String name,String description,BigDecimal price,int stock){this(shopId,categoryId,name,description,price,stock,null,List.of(new SkuService.CreateRequest("默认规格", price, stock)));}
		public CreateProductRequest { skus=skus==null?List.of():List.copyOf(skus); }
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
		@Positive
		private Long imageId;
		private boolean categoryIdSpecified;
		private boolean nameSpecified;
		private boolean descriptionSpecified;
		private boolean priceSpecified;
		private boolean stockSpecified;
		private boolean statusSpecified;
		private boolean versionSpecified;
		private boolean imageIdSpecified;

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
			this.categoryIdSpecified = true;
			markUpdateSpecified();
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			this.nameSpecified = true;
			markUpdateSpecified();
		}

		public String description() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
			this.descriptionSpecified = true;
			markUpdateSpecified();
		}

		public BigDecimal price() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
			this.priceSpecified = true;
			markUpdateSpecified();
		}

		public Integer stock() {
			return stock;
		}

		public void setStock(Integer stock) {
			this.stock = stock;
			this.stockSpecified = true;
			markUpdateSpecified();
		}

		public String status() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
			this.statusSpecified = true;
			markUpdateSpecified();
		}

		public Long version() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
			this.versionSpecified = true;
			markUpdateSpecified();
		}

		public Long imageId() { return imageId; }
		public void setImageId(Long imageId) { this.imageId = imageId; this.imageIdSpecified = true; markUpdateSpecified(); }

		@JsonIgnore
		public boolean isCategoryIdSpecified() {
			return categoryIdSpecified;
		}

		@JsonIgnore
		public boolean isNameSpecified() {
			return nameSpecified;
		}

		@JsonIgnore
		public boolean isDescriptionSpecified() {
			return descriptionSpecified;
		}

		@JsonIgnore
		public boolean isPriceSpecified() {
			return priceSpecified;
		}

		@JsonIgnore
		public boolean isStockSpecified() {
			return stockSpecified;
		}

		@JsonIgnore
		public boolean isStatusSpecified() {
			return statusSpecified;
		}

		@JsonIgnore
		public boolean isVersionSpecified() {
			return versionSpecified;
		}

		@JsonIgnore
		public boolean isImageIdSpecified() { return imageIdSpecified; }
	}

	record ProductQuery(Long categoryId, String keyword, Integer page, Integer pageSize, String sortBy,
		String sortOrder, Boolean includeOffSale, Long merchantId) {
	}

	record CategoryView(long id, long shopId, String name, int sortOrder, Instant createdAt, Instant updatedAt) {
	}

	record ProductView(long id, long shopId, long categoryId, String name, String description, BigDecimal minPrice,
			int stock, String status, long version, Instant createdAt, Instant updatedAt, ImageView image, List<SkuService.SkuView> skus) {
		public ProductView(long id,long shopId,long categoryId,String name,String description,BigDecimal price,int stock,String status,long version,Instant createdAt,Instant updatedAt){this(id,shopId,categoryId,name,description,price,stock,status,version,createdAt,updatedAt,null,List.of());}
		public ProductView { skus=skus==null?List.of():List.copyOf(skus); }
		public boolean inStock(){return stock > 0;}
		public BigDecimal price(){return minPrice;}
	}
	record ImageView(long id,String url,String contentType,long size,Instant createdAt) { public ImageView(long id,String url){this(id,url,null,0,null);} }

	record ReservationRequest(long productId, long expectedVersion, int quantity, long skuId) {
		public ReservationRequest(long productId, long expectedVersion, int quantity) { this(productId, expectedVersion, quantity, 0); }
	}

	record ProductSnapshot(long productId, long shopId, String name, BigDecimal unitPrice, int quantity,
			long version, long skuId, String skuName, String imageUrl) {
		public ProductSnapshot(long productId, long shopId, String name, BigDecimal unitPrice, int quantity, long version) {
			this(productId, shopId, name, unitPrice, quantity, version, 0, null, null);
		}
	}

	record StockRestore(long productId, int quantity, long skuId) {
		public StockRestore(long productId, int quantity) { this(productId, quantity, 0); }
	}
}
