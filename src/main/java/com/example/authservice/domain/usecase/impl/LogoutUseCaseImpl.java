package com.example.authservice.domain.usecase.impl;

import com.example.authservice.domain.repository.TokenRepository;
import com.example.authservice.domain.usecase.LogoutUseCase;
import com.example.authservice.infrastructure.service.JwtService;

public class LogoutUseCaseImpl implements LogoutUseCase {
    private final TokenRepository tokenRepository;

    public LogoutUseCaseImpl(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void logout(String token) {
        JwtService.validateToken(token);
        var obj = this.tokenRepository.getTokenByToken(token);
        obj.ifPresent(tokenObj -> this.tokenRepository.deleteToken(tokenObj.getId()));
    }
}