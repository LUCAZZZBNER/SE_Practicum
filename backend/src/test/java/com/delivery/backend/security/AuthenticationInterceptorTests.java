package com.delivery.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;

class AuthenticationInterceptorTests {

	private JwtTokenService tokenService;
	private AuthenticationInterceptor interceptor;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		tokenService = org.mockito.Mockito.mock(JwtTokenService.class);
		interceptor = new AuthenticationInterceptor(tokenService);
		request = new MockHttpServletRequest();
	}

	@Test
	void publicEndpointIgnoresAuthenticationAndOptionalEndpointAllowsAnonymousAccess() throws Exception {
		request.addHeader("Authorization", "invalid value ignored by public login");
		assertThat(preHandle("publicEndpoint")).isTrue();
		request = new MockHttpServletRequest();
		assertThat(preHandle("optionalEndpoint")).isTrue();
		verifyNoInteractions(tokenService);
	}

	@Test
	void optionalAuthenticationParsesTokenAndExposesPrincipalWhenHeaderIsPresent() throws Exception {
		CurrentPrincipal principal = new CurrentPrincipal(2, Role.MERCHANT);
		request.addHeader("Authorization", "Bearer token");
		when(tokenService.parse("token")).thenReturn(principal);

		assertThat(preHandle("optionalEndpoint")).isTrue();
		assertThat(request.getAttribute(AuthenticationInterceptor.CURRENT_PRINCIPAL_ATTRIBUTE)).isEqualTo(principal);
		verify(tokenService).parse("token");
	}

	@Test
	void protectedEndpointRejectsMissingMalformedAndWrongRoleAuthentication() {
		assertFailure(ApiError.UNAUTHENTICATED, () -> preHandle("userEndpoint"));

		request.addHeader("Authorization", "Basic token");
		assertFailure(ApiError.UNAUTHENTICATED, () -> preHandle("userEndpoint"));

		request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer  ");
		assertFailure(ApiError.UNAUTHENTICATED, () -> preHandle("userEndpoint"));

		request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer two tokens");
		assertFailure(ApiError.UNAUTHENTICATED, () -> preHandle("userEndpoint"));

		request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer merchant-token");
		when(tokenService.parse("merchant-token")).thenReturn(new CurrentPrincipal(2, Role.MERCHANT));
		assertFailure(ApiError.FORBIDDEN, () -> preHandle("userEndpoint"));
	}

	@Test
	void matchingRoleMayProceedAndNonHandlerResourcesAreIgnored() throws Exception {
		CurrentPrincipal principal = new CurrentPrincipal(7, Role.USER);
		request.addHeader("Authorization", "Bearer user-token");
		when(tokenService.parse("user-token")).thenReturn(principal);

		assertThat(preHandle("userEndpoint")).isTrue();
		assertThat(request.getAttribute(AuthenticationInterceptor.CURRENT_PRINCIPAL_ATTRIBUTE)).isEqualTo(principal);
		assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
	}

	private boolean preHandle(String methodName) throws Exception {
		HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
		return interceptor.preHandle(request, new MockHttpServletResponse(), handler);
	}

	private static void assertFailure(ApiError error, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.error()).isEqualTo(error));
	}

	static class TestController {
		@PublicEndpoint
		public void publicEndpoint() {
		}

		@PublicEndpoint(optionalAuthentication = true)
		public void optionalEndpoint() {
		}

		@RequireRole(Role.USER)
		public void userEndpoint() {
		}
	}
}
