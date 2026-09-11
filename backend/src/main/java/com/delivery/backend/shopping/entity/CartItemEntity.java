package com.delivery.backend.shopping.entity;

import java.math.BigDecimal;
import java.time.Instant;

/** Cart row plus the latest product/shop data loaded for display and checkout. */
public class CartItemEntity {

	private Long id;
	private Long userId;
	private Long productId;
	private Long skuId;
	private Integer quantity;
	private Instant createdAt;
	private Instant updatedAt;
	private Long shopId;
	private String productName;
	private BigDecimal productPrice;
	private Integer productStock;
	private String productStatus;
	private Long productVersion;
	private String shopStatus;
	private String productImageUrl;
	private String skuName; private BigDecimal skuPrice; private Integer skuStock; private String skuStatus; private Long skuVersion;
	public Long getSkuId(){return skuId;} public void setSkuId(Long v){skuId=v;}
	public String getProductImageUrl(){return productImageUrl;} public void setProductImageUrl(String v){productImageUrl=v;}
	public String getSkuName(){return skuName;} public void setSkuName(String v){skuName=v;}
	public BigDecimal getSkuPrice(){return skuPrice;} public void setSkuPrice(BigDecimal v){skuPrice=v;}
	public Integer getSkuStock(){return skuStock;} public void setSkuStock(Integer v){skuStock=v;}
	public String getSkuStatus(){return skuStatus;} public void setSkuStatus(String v){skuStatus=v;}
	public Long getSkuVersion(){return skuVersion;} public void setSkuVersion(Long v){skuVersion=v;}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public BigDecimal getProductPrice() {
		return productPrice;
	}

	public void setProductPrice(BigDecimal productPrice) {
		this.productPrice = productPrice;
	}

	public Integer getProductStock() {
		return productStock;
	}

	public void setProductStock(Integer productStock) {
		this.productStock = productStock;
	}

	public String getProductStatus() {
		return productStatus;
	}

	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}

	public Long getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(Long productVersion) {
		this.productVersion = productVersion;
	}

	public String getShopStatus() {
		return shopStatus;
	}

	public void setShopStatus(String shopStatus) {
		this.shopStatus = shopStatus;
	}
}
