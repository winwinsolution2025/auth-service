package com.example.authservice.domain.gateway.publisher;

public interface MessagePublisher {
    void publish(String subject, String message);
}
