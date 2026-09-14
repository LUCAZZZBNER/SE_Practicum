package com.delivery.backend.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OrderAddressSnapshotCodecTests {

	@Test
	void readsJsonNormalizedByMysqlWithWhitespaceAfterColons() {
		Map<String, String> snapshot = OrderAddressSnapshotCodec.read(
				"{\"detail\": \"文三路 1 号\", \"phone\": \"13800000000\", \"recipient\": \"张三\", \"region\": \"杭州\"}");

		assertThat(snapshot).containsEntry("recipient", "张三")
				.containsEntry("phone", "13800000000")
				.containsEntry("region", "杭州")
				.containsEntry("detail", "文三路 1 号");
	}

	@Test
	void treatsAnEmptyLegacyJsonObjectAsAMissingSnapshot() {
		assertThat(OrderAddressSnapshotCodec.read("{}")).isNull();
	}

	@Test
	void roundTripsAddressCharactersThatRequireJsonEscaping() {
		String json = OrderAddressSnapshotCodec.write(
				"recipient", "张\"三", "phone", "13800000000", "region", "杭州", "detail", "A\\1 号");

		assertThat(OrderAddressSnapshotCodec.read(json)).hasSize(4).containsAllEntriesOf(Map.of(
				"recipient", "张\"三", "phone", "13800000000", "region", "杭州", "detail", "A\\1 号"));
	}
}
