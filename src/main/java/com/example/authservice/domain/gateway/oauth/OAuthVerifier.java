package com.example.authservice.domain.gateway.oauth;

public interface OAuthVerifier {
    OAuthUser verify(String oAuthToken) throws Exception;
}
