package com.delivery.backend.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.delivery.backend.order.entity.OrderEntity;
import com.delivery.backend.order.entity.OrderItemEntity;
import com.delivery.backend.order.entity.PaymentEntity;
import com.delivery.backend.order.entity.RefundEntity;

/** MyBatis data-access contract for order headers, line snapshots, and history queries. */
@Mapper
public interface OrderDao {

	OrderEntity findByUserAndIdempotency(@Param("userId") long userId,
			@Param("idempotencyKey") String idempotencyKey);

	OrderEntity findMine(@Param("userId") long userId, @Param("orderId") long orderId);

	OrderEntity findMerchantOrder(@Param("merchantId") long merchantId,
			@Param("orderId") long orderId);

	int insertOrder(OrderEntity order);

	int insertItems(@Param("items") List<OrderItemEntity> items);

	List<OrderItemEntity> listItems(@Param("orderId") long orderId);

	List<OrderEntity> listMine(@Param("userId") long userId,
			@Param("status") String status,
			@Param("sortBy") String sortBy,
			@Param("sortOrder") String sortOrder,
			@Param("limit") int limit,
			@Param("offset") long offset);

	long countMine(@Param("userId") long userId, @Param("status") String status);

	List<OrderEntity> listMerchant(@Param("merchantId") long merchantId,
			@Param("shopId") Long shopId,
			@Param("status") String status,
			@Param("sortBy") String sortBy,
			@Param("sortOrder") String sortOrder,
			@Param("limit") int limit,
			@Param("offset") long offset);

	long countMerchant(@Param("merchantId") long merchantId,
			@Param("shopId") Long shopId,
			@Param("status") String status);

	int cancelPending(@Param("userId") long userId, @Param("orderId") long orderId);

	int transitionStatus(@Param("orderId") long orderId, @Param("status") String status,
			@Param("fromStatus") String fromStatus);

	int cancelEligible(@Param("userId") long userId, @Param("orderId") long orderId, @Param("reason") String reason,
			@Param("refund") boolean refund);

	PaymentEntity findPayment(@Param("orderId") long orderId);
	PaymentEntity findPaymentByKey(@Param("idempotencyKey") String idempotencyKey);
	int insertPayment(PaymentEntity payment);
	RefundEntity findRefund(@Param("orderId") long orderId);
	RefundEntity findRefundByKey(@Param("idempotencyKey") String idempotencyKey);
	int insertRefund(RefundEntity refund);
}
