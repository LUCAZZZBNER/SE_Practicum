package com.delivery.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTests {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@ParameterizedTest
	@MethodSource("businessErrors")
	void everyDocumentedBusinessErrorPreservesItsHttpStatusCodeMessageAndData(ApiError error) {
		Map<String, Object> data = Map.of("context", error.name());
		ResponseEntity<ApiResponse<Object>> response = handler.businessFailure(new BusinessException(error, data));

		assertThat(response.getStatusCode()).isEqualTo(error.status());
		assertThat(response.getBody()).isEqualTo(new ApiResponse<>(error.code(), error.message(), data));
	}

	@Test
	void unexpectedExceptionsAreSanitized() {
		ResponseEntity<ApiResponse<Object>> response = handler.unexpectedFailure(
				new RuntimeException("password=secret; database details"));

		assertThat(response.getStatusCode()).isEqualTo(ApiError.INTERNAL_ERROR.status());
		assertThat(response.getBody()).isEqualTo(new ApiResponse<>(9000, "服务器内部错误", null));
		assertThat(response.toString()).doesNotContain("password", "database", "secret");
	}

	@Test
	void oversizedMultipartUsesTheDocumentedImageError() {
		ResponseEntity<ApiResponse<Object>> response = handler.uploadTooLarge(
				new MaxUploadSizeExceededException(5L * 1024 * 1024));

		assertThat(response.getStatusCode()).isEqualTo(ApiError.IMAGE_INVALID.status());
		assertThat(response.getBody()).isEqualTo(new ApiResponse<>(1405, "图片格式或大小不合法", null));
	}

	private static Stream<ApiError> businessErrors() {
		return Stream.of(ApiError.values()).filter(error -> error != ApiError.INTERNAL_ERROR);
	}
}
