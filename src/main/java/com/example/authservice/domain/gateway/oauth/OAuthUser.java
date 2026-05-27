package com.example.authservice.domain.gateway.oauth;

public interface OAuthUser {
    String getProviderId();

    String getEmail();

    String getName();

    String getAvatar();
}
