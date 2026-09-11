package com.delivery.backend.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class RefundEntity {
	private Long id;
	private Long orderId;
	private String refundNumber;
	private BigDecimal amount;
	private String status;
	private String idempotencyKey;
	private Instant completedAt;
	private Instant createdAt;
	public Long getId(){return id;} public void setId(Long v){id=v;}
	public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
	public String getRefundNumber(){return refundNumber;} public void setRefundNumber(String v){refundNumber=v;}
	public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
	public String getStatus(){return status;} public void setStatus(String v){status=v;}
	public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
	public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;}
	public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
