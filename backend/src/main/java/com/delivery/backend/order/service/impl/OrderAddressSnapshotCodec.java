package com.delivery.backend.order.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Encodes immutable order address snapshots and reads MySQL-normalized JSON. */
final class OrderAddressSnapshotCodec {

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
	private static final TypeReference<Map<String, String>> SNAPSHOT_TYPE = new TypeReference<>() { };

	private OrderAddressSnapshotCodec() {
	}

	static String write(String... values) {
		if (values.length % 2 != 0) {
			throw new IllegalArgumentException("Snapshot values must contain key/value pairs");
		}
		Map<String, String> snapshot = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			snapshot.put(values[i], values[i + 1]);
		}
		try {
			return JSON_MAPPER.writeValueAsString(snapshot);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to encode order address snapshot", exception);
		}
	}

	static Map<String, String> read(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			Map<String, String> snapshot = JSON_MAPPER.readValue(value, SNAPSHOT_TYPE);
			return snapshot == null || snapshot.isEmpty() ? null : snapshot;
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to decode persisted order address snapshot", exception);
		}
	}
}
