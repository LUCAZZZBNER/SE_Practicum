package com.delivery.backend.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

/** Persistence model for an order header and its immutable display snapshots. */
public class OrderEntity {

	private Long id;
	private String orderNumber;
	private Long userId;
	private String idempotencyKey;
	private String requestFingerprint;
	private Long shopId;
	private String shopName;
	private BigDecimal totalAmount;
	private String status;
	private Instant createdAt;
	private Instant updatedAt;
	private Instant cancelledAt;
	private String paymentStatus;
	private String refundStatus;
	private String remark;
	private String cancelReason;
	private Instant completedAt;
	private String userAddressSnapshot;
	private String shopAddressSnapshot;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public String getRequestFingerprint() {
		return requestFingerprint;
	}

	public void setRequestFingerprint(String requestFingerprint) {
		this.requestFingerprint = requestFingerprint;
	}

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
	}
	public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
	public String getRefundStatus(){return refundStatus;} public void setRefundStatus(String v){refundStatus=v;}
	public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
	public String getCancelReason(){return cancelReason;} public void setCancelReason(String v){cancelReason=v;}
	public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;}
	public String getUserAddressSnapshot(){return userAddressSnapshot;} public void setUserAddressSnapshot(String v){userAddressSnapshot=v;}
	public String getShopAddressSnapshot(){return shopAddressSnapshot;} public void setShopAddressSnapshot(String v){shopAddressSnapshot=v;}
}
