package com.delivery.identity.config;

import com.delivery.identity.security.AuthenticationInterceptor;
import com.delivery.identity.security.InternalServiceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthenticationInterceptor authenticationInterceptor;
  private final InternalServiceInterceptor internalServiceInterceptor;

  public WebMvcConfig(
      AuthenticationInterceptor authenticationInterceptor,
      InternalServiceInterceptor internalServiceInterceptor) {
    this.authenticationInterceptor = authenticationInterceptor;
    this.internalServiceInterceptor = internalServiceInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authenticationInterceptor).addPathPatterns("/api/v1/**");
    registry.addInterceptor(internalServiceInterceptor).addPathPatterns("/internal/**");
  }
}
