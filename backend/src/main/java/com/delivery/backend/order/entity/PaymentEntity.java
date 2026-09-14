package com.delivery.backend.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentEntity {
	private Long id;
	private Long orderId;
	private String paymentNumber;
	private BigDecimal amount;
	private String status;
	private String idempotencyKey;
	private Instant paidAt;
	private Instant createdAt;
	public Long getId(){return id;} public void setId(Long v){id=v;}
	public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
	public String getPaymentNumber(){return paymentNumber;} public void setPaymentNumber(String v){paymentNumber=v;}
	public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
	public String getStatus(){return status;} public void setStatus(String v){status=v;}
	public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
	public Instant getPaidAt(){return paidAt;} public void setPaidAt(Instant v){paidAt=v;}
	public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
