package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {
    private String token;

    @JsonCreator
    public LoginResponse(
            @JsonProperty("token") String token
    ) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
} 