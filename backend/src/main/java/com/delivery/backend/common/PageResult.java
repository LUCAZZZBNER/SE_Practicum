package com.delivery.backend.common;

import java.util.List;

/** Typed pagination result shared by service contracts and HTTP responses. */
public record PageResult<T>(List<T> items, int page, int pageSize, long total, int totalPages) {

	public PageResult {
		items = List.copyOf(items);
	}
}
