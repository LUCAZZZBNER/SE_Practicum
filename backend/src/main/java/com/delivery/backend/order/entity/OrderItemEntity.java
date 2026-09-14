package com.delivery.backend.order.entity;

import java.math.BigDecimal;

/** Persistence model for one immutable order-line snapshot. */
public class OrderItemEntity {

	private Long id;
	private Long orderId;
	private Long productId;
	private String productName;
	private BigDecimal unitPrice;
	private Integer quantity;
	private Long skuId;
	private String skuName;
	private String imageUrl;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public Long getSkuId(){return skuId;} public void setSkuId(Long v){skuId=v;}
	public String getSkuName(){return skuName;} public void setSkuName(String v){skuName=v;}
	public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
}
