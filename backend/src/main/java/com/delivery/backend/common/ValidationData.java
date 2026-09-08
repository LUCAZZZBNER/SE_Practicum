package com.delivery.backend.common;

import java.util.Map;

/** Field-level validation details returned with error code 1001. */
public record ValidationData(Map<String, String> fieldErrors) {

	public ValidationData {
		fieldErrors = Map.copyOf(fieldErrors);
	}
}
