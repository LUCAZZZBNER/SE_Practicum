package com.delivery.backend;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import com.delivery.backend.common.GlobalExceptionHandler;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.Role;

public final class ControllerTestSupport {

	public static final String JSON = "application/json";

	private ControllerTestSupport() {
	}

	public static ResultMatcher successfulDataId(long id) {
		return result -> {
			jsonPath("$.code").value(0).match(result);
			jsonPath("$.msg").value("操作成功").match(result);
			jsonPath("$.data.id").value(id).match(result);
		};
	}

	public static ResultMatcher successfulEnvelope() {
		return result -> {
			jsonPath("$.code").value(0).match(result);
			jsonPath("$.msg").value("操作成功").match(result);
		};
	}

	public static ResultMatcher successfulPage(int page, int pageSize, long total) {
		return result -> {
			successfulEnvelope().match(result);
			jsonPath("$.data.page").value(page).match(result);
			jsonPath("$.data.pageSize").value(pageSize).match(result);
			jsonPath("$.data.total").value(total).match(result);
		};
	}

	public static CurrentPrincipal userPrincipal(long id) {
		return new CurrentPrincipal(id, Role.USER);
	}

	public static CurrentPrincipal merchantPrincipal(long id) {
		return new CurrentPrincipal(id, Role.MERCHANT);
	}

	public static StandaloneMockMvcBuilder withApiErrors(Object controller) {
		JsonMapper mapper = JsonMapper.builder()
				.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
		return org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
				.setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
				.setControllerAdvice(new GlobalExceptionHandler());
	}

	public static ResultMatcher validationError() {
		return result -> {
			jsonPath("$.code").value(1001).match(result);
			jsonPath("$.msg").value("请求参数不合法").match(result);
		};
	}

	public static ResultMatcher unauthenticated() {
		return result -> {
			jsonPath("$.code").value(1002).match(result);
			jsonPath("$.msg").value("未携带、过期或无效令牌").match(result);
			jsonPath("$.data").doesNotExist().match(result);
		};
	}
}
