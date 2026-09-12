package com.delivery.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PersistenceSchemaContractTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void cartSkuReferenceIsRequired() {
		String nullable = jdbcTemplate.queryForObject("""
				SELECT IS_NULLABLE
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = 'cart_items'
				  AND COLUMN_NAME = 'sku_id'
				""", String.class);

		assertThat(nullable).isEqualTo("NO");
	}

	@Test
	void imagesStoreTheirOwningMerchant() {
		Integer columns = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = 'images'
				  AND COLUMN_NAME = 'merchant_id'
				""", Integer.class);

		assertThat(columns).isEqualTo(1);
	}

	@Test
	void orderActionsHaveAScopedIdempotencyTable() {
		Integer tables = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.TABLES
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = 'order_action_idempotency'
				""", Integer.class);

		assertThat(tables).isEqualTo(1);
	}
}
