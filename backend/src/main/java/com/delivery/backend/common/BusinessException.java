package com.delivery.backend.common;

import java.util.Objects;

/** Domain failure that the global handler converts into the public API envelope. */
public class BusinessException extends RuntimeException {

	private final ApiError error;
	private final Object data;

	public BusinessException(ApiError error) {
		this(error, null);
	}

	public BusinessException(ApiError error, Object data) {
		super(Objects.requireNonNull(error).message());
		this.error = error;
		this.data = data;
	}

	public ApiError error() {
		return error;
	}

	public Object data() {
		return data;
	}
}
