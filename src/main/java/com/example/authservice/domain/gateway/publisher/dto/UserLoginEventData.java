package com.example.authservice.domain.gateway.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserLoginEventData {
    @JsonProperty("user_id")
    private int userId;
    private String email;

    public UserLoginEventData(int userId, String email) {
        this.userId = userId;
        this.email = email;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
