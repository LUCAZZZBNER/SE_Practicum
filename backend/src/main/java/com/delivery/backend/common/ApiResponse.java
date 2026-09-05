package com.delivery.backend.common;

/** Unified response envelope defined by the backend API contract. */
public record ApiResponse<T>(int code, String msg, T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(0, "操作成功", data);
	}
}
