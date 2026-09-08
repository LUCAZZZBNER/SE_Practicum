package com.delivery.backend.security;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

	public static final String CURRENT_PRINCIPAL_ATTRIBUTE = "currentPrincipal";

	private final JwtTokenService tokenService;

	public AuthenticationInterceptor(JwtTokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod method)) {
			return true;
		}
		PublicEndpoint publicEndpoint = findPublicEndpoint(method);
		String authorization = request.getHeader("Authorization");
		if (publicEndpoint != null && !publicEndpoint.optionalAuthentication()) {
			return true;
		}
		if ((authorization == null || authorization.isBlank()) && publicEndpoint != null) {
			return true;
		}
		CurrentPrincipal principal = tokenService.parse(extractBearerToken(authorization));
		RequireRole requiredRole = findRequiredRole(method);
		if (requiredRole != null && principal.role() != requiredRole.value()) {
			throw new BusinessException(ApiError.FORBIDDEN);
		}
		request.setAttribute(CURRENT_PRINCIPAL_ATTRIBUTE, principal);
		return true;
	}

	private static String extractBearerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new BusinessException(ApiError.UNAUTHENTICATED);
		}
		String token = authorization.substring("Bearer ".length());
		if (token.isBlank() || token.indexOf(' ') >= 0) {
			throw new BusinessException(ApiError.UNAUTHENTICATED);
		}
		return token;
	}

	private static PublicEndpoint findPublicEndpoint(HandlerMethod method) {
		PublicEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), PublicEndpoint.class);
		return annotation != null ? annotation
				: AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), PublicEndpoint.class);
	}

	private static RequireRole findRequiredRole(HandlerMethod method) {
		RequireRole annotation = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
		return annotation != null ? annotation
				: AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
	}
}
