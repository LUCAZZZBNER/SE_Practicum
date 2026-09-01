package com.delivery.backend.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

/** Entry point for transactional checkout and order state transitions. */
@Service
public class OrderModule {
	public BigDecimal calculateTotal(List<OrderLine> lines) {
		if (lines == null || lines.isEmpty()) {
			throw new IllegalArgumentException("at least one order line is required");
		}
		return lines.stream()
				.map(OrderLine::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public boolean canCancel(OrderStatus status) {
		return status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAID;
	}
}
