package com.example.authservice.domain.usecase.impl;

import com.example.authservice.domain.entity.Token;
import com.example.authservice.domain.repository.TokenRepository;
import com.example.authservice.domain.usecase.VerifyTokenUseCase;
import com.example.authservice.infrastructure.service.JwtService;

import java.time.Instant;
import java.util.Optional;


public class VerifyTokenUseCaseImpl implements VerifyTokenUseCase {
    private final TokenRepository tokenRepository;

    public VerifyTokenUseCaseImpl(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public boolean verifyToken(String token) {
        var valid = JwtService.validateToken(token);
        if (!valid) {
            return false;
        }

        Optional<Token> tokenObj = this.tokenRepository.getTokenByToken(token);
        if (tokenObj.isEmpty()) {
            return false;
        } else if (tokenObj.get().getExpired_at().isBefore(Instant.now())) {
            this.tokenRepository.deleteToken(tokenObj.get().getId());
            return false;
        }

        return true;
    }
} 