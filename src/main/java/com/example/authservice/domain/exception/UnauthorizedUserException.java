package com.example.authservice.domain.exception;

public class UnauthorizedUserException extends HttpException {
    public UnauthorizedUserException() {
        super("Unauthorized User", 401);
    }

    public UnauthorizedUserException(String msg) {
        super(msg, 401);
    }
} 