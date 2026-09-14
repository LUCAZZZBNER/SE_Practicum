package com.delivery.backend.address;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
@Transactional
class UserAddressServiceContractTests extends ServiceContractTestSupport {
	@Autowired private UserAddressService service;
	@Autowired private UserService users;

	@Test
	void firstAddressBecomesDefaultEvenWhenTheFlagIsOmitted() {
		long userId = users.register(new UserService.RegisterRequest("address-default-" + System.nanoTime(),
				"ExamplePass123!", "ExamplePass123!", "Alice", null)).id();
		UserAddressService.AddressView address = service.create(userId,
				new UserAddressService.CreateRequest("张三", "13800000000", "杭州", "学院路", false));
		assertThat(address.isDefault()).isTrue();
	}
}
