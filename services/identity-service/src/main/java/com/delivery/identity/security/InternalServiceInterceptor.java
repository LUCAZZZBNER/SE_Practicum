package com.delivery.identity.security;

import com.delivery.identity.common.ApiError;
import com.delivery.identity.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalServiceInterceptor implements HandlerInterceptor {
  private final String expectedToken;

  public InternalServiceInterceptor(@Value("${security.internal-service-token}") String expectedToken) {
    this.expectedToken = expectedToken;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String supplied = request.getHeader("X-Service-Token");
    if (supplied == null || expectedToken == null || !java.security.MessageDigest.isEqual(
        supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        expectedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      throw new BusinessException(ApiError.UNAUTHENTICATED);
    }
    return true;
  }
}
