package com.delivery.backend.common;

import org.springframework.http.HttpStatus;

/** Stable HTTP and business-code mapping defined by the backend API contract. */
public enum ApiError {
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 1001, "请求参数不合法"),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, 1002, "未携带、过期或无效令牌"),
	FORBIDDEN(HttpStatus.FORBIDDEN, 1003, "已登录但角色或资源归属不符"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 1004, "资源不存在"),
	RESOURCE_CONFLICT(HttpStatus.CONFLICT, 1005, "资源冲突"),
	ACCOUNT_EXISTS(HttpStatus.CONFLICT, 1101, "用户账号已存在"),
	BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1102, "账号或密码错误"),
	ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, 1103, "用户账号已禁用"),
	MERCHANT_ACCOUNT_EXISTS(HttpStatus.CONFLICT, 1201, "商家账号已存在"),
	MERCHANT_SUSPENDED(HttpStatus.FORBIDDEN, 1202, "商家已暂停"),
	SHOP_NOT_OPEN(HttpStatus.CONFLICT, 1301, "店铺未营业"),
	PRODUCT_OFF_SALE(HttpStatus.CONFLICT, 1401, "商品已下架"),
	INSUFFICIENT_STOCK(HttpStatus.CONFLICT, 1402, "库存不足"),
	CART_EMPTY(HttpStatus.BAD_REQUEST, 1501, "购物车为空"),
	MIXED_SHOPS(HttpStatus.BAD_REQUEST, 1502, "一次结算不能包含多个店铺"),
	PRICE_CHANGED(HttpStatus.CONFLICT, 1601, "商品价格或版本已变化"),
	ORDER_STATE_CONFLICT(HttpStatus.CONFLICT, 1602, "当前订单状态不允许该操作"),
	IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, 1603, "幂等键对应的请求不一致"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 9000, "服务器内部错误");

	private final HttpStatus status;
	private final int code;
	private final String message;

	ApiError(HttpStatus status, int code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public int code() {
		return code;
	}

	public String message() {
		return message;
	}
}
