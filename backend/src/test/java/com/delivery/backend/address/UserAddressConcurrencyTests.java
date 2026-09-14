package com.delivery.backend.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
class UserAddressConcurrencyTests {

	@Autowired
	private UserAddressService addressService;
	@Autowired
	private UserService userService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void concurrentFirstAddressesLeaveExactlyOneDefaultAddress() throws Exception {
		long userId = createUser("address-concurrent");
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> first = executor.submit(() -> createAfter(start, userId, "学院路 1 号"));
			Future<?> second = executor.submit(() -> createAfter(start, userId, "学院路 2 号"));
			start.countDown();
			first.get();
			second.get();
		} finally {
			executor.shutdownNow();
		}

		List<UserAddressService.AddressView> addresses = addressService.list(userId);
		assertThat(addresses).hasSize(2);
		assertThat(addresses).filteredOn(UserAddressService.AddressView::isDefault).hasSize(1);
	}

	@Test
	@Transactional
	void databaseRejectsTwoActiveDefaultAddressesForTheSameUser() {
		long userId = createUser("address-db-unique");
		addressService.create(userId,
				new UserAddressService.CreateRequest("张三", "13800000000", "杭州", "学院路 1 号", true));

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO user_addresses(user_id,recipient,phone,region,detail,is_default)
				VALUES (?, '李四', '13900000000', '杭州', '学院路 2 号', TRUE)
				""", userId)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private long createUser(String label) {
		return userService.register(new UserService.RegisterRequest(label + "-" + System.nanoTime(),
				"ExamplePass123!", "ExamplePass123!", "Address User", null)).id();
	}

	private void createAfter(CountDownLatch start, long userId, String detail) {
		try {
			start.await();
			addressService.create(userId,
					new UserAddressService.CreateRequest("张三", "13800000000", "杭州", detail, false));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}
}
