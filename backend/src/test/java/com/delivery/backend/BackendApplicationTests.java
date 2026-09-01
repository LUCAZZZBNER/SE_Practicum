package com.delivery.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void moduleBoundariesAreValid() {
		ApplicationModules.of(BackendApplication.class).verify();
	}

}
