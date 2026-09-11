package com.delivery.backend.address.entity;

import java.time.Instant;

public class UserAddressEntity {
	private Long id;
	private Long userId;
	private String recipient;
	private String phone;
	private String region;
	private String detail;
	private Boolean defaultAddress;
	private Instant createdAt;
	private Instant updatedAt;
	private Instant deletedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	public String getRecipient() { return recipient; }
	public void setRecipient(String recipient) { this.recipient = recipient; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }
	public String getDetail() { return detail; }
	public void setDetail(String detail) { this.detail = detail; }
	public Boolean getDefaultAddress() { return defaultAddress; }
	public void setDefaultAddress(Boolean defaultAddress) { this.defaultAddress = defaultAddress; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
	public Instant getDeletedAt() { return deletedAt; }
	public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
