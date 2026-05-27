package com.example.authservice.infrastructure.service.nats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthUserRegisterEvent {
    @JsonProperty("user_id")
    private int userId;
    private String email;

    public AuthUserRegisterEvent(int userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public int getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
