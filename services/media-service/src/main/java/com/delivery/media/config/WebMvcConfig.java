package com.delivery.media.config;

import com.delivery.media.security.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthenticationInterceptor authenticationInterceptor;

  public WebMvcConfig(AuthenticationInterceptor authenticationInterceptor) {
    this.authenticationInterceptor = authenticationInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authenticationInterceptor).addPathPatterns("/api/v1/**");
  }
}
