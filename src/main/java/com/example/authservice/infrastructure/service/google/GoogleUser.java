package com.example.authservice.infrastructure.service.google;

import com.example.authservice.domain.gateway.oauth.OAuthUser;

public class GoogleUser implements OAuthUser {
    private final String googleId;
    private final String email;
    private final String name;
    private final String avatar;

    public GoogleUser(String googleId, String email, String name, String avatar) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.avatar = avatar;
    }

    @Override
    public String getProviderId() {
        return googleId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }
}
