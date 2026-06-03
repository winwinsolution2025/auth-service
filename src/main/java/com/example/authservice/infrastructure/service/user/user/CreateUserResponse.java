package com.example.authservice.infrastructure.service.user.user;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserResponse {
    public UserResponse data;
    public CreateUserResponse() {
    }
}


