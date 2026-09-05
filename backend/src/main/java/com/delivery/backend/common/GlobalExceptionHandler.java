package com.delivery.backend.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ServletRequestBindingException.class)
	public ResponseEntity<ApiResponse<Object>> unauthenticated(ServletRequestBindingException exception) {
		return error(ApiError.UNAUTHENTICATED, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> invalidBody(MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return error(ApiError.VALIDATION_ERROR, new ValidationData(fieldErrors));
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class,
			MissingRequestHeaderException.class, HttpMessageNotReadableException.class,
			HandlerMethodValidationException.class })
	public ResponseEntity<ApiResponse<Object>> invalidRequest(Exception exception) {
		return error(ApiError.VALIDATION_ERROR, null);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> businessFailure(BusinessException exception) {
		return error(exception.error(), exception.data());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> unexpectedFailure(Exception exception) {
		LOGGER.error("Unhandled backend exception type: {}", exception.getClass().getName());
		return error(ApiError.INTERNAL_ERROR, null);
	}

	private static ResponseEntity<ApiResponse<Object>> error(ApiError error, Object data) {
		return ResponseEntity.status(error.status())
				.body(new ApiResponse<>(error.code(), error.message(), data));
	}
}
