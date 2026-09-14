package com.delivery.backend.order.entity;

import java.time.Instant;

/** Persistence record that scopes an idempotency key to one actor and action. */
public class OrderActionIdempotencyEntity {
	private Long id;
	private String actorType;
	private Long actorId;
	private String actionName;
	private String idempotencyKey;
	private String requestFingerprint;
	private Long orderId;
	private Instant createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getActorType() { return actorType; }
	public void setActorType(String actorType) { this.actorType = actorType; }
	public Long getActorId() { return actorId; }
	public void setActorId(Long actorId) { this.actorId = actorId; }
	public String getActionName() { return actionName; }
	public void setActionName(String actionName) { this.actionName = actionName; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
	public String getRequestFingerprint() { return requestFingerprint; }
	public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
	public Long getOrderId() { return orderId; }
	public void setOrderId(Long orderId) { this.orderId = orderId; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
