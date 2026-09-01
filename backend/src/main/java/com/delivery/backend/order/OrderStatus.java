package com.delivery.backend.order;

/** Order state machine used by the order module. */
public enum OrderStatus {
	PENDING_PAYMENT,
	PAID,
	PREPARING,
	COMPLETED,
	CANCELLED
}
