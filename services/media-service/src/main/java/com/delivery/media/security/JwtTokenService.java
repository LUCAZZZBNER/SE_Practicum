package com.delivery.media.security;

public interface JwtTokenService {

  TokenSession issue(long principalId, Role role);

  CurrentPrincipal parse(String token);

  record TokenSession(String accessToken, String tokenType, long expiresIn) {}
}
