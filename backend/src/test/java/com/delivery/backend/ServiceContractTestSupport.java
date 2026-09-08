package com.delivery.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;

public abstract class ServiceContractTestSupport {

	protected static void assertBusinessError(ApiError expected,
			org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.error()).isEqualTo(expected));
	}
}
