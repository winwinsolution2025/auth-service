package com.example.authservice.domain.usecase.impl;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.entity.Token;
import com.example.authservice.domain.exception.ConflictResourceException;
import com.example.authservice.domain.exception.InternalServerException;
import com.example.authservice.domain.exception.NotFoundUserException;
import com.example.authservice.domain.exception.UnauthorizedUserException;
import com.example.authservice.domain.repository.AuthUserRepository;
import com.example.authservice.domain.repository.TokenRepository;
import com.example.authservice.domain.gateway.publisher.MessagePublisher;
import com.example.authservice.domain.gateway.publisher.dto.ExternalEvent;
import com.example.authservice.domain.gateway.publisher.dto.UserLoginEventData;
import com.example.authservice.domain.usecase.LoginByGoogleUseCase;
import com.example.authservice.domain.usecase.RegisterUseCase;
import com.example.authservice.dto.UserRegisterRequest;
import com.example.authservice.infrastructure.service.nats.AuthUserLoginEvent;
import com.example.authservice.infrastructure.service.nats.NatsJetStreamClient;
import com.example.authservice.infrastructure.service.JwtService;
import com.example.authservice.domain.gateway.oauth.OAuthUser;
import com.example.authservice.domain.gateway.oauth.OAuthVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

public class LoginByGoogleUseCaseImpl implements LoginByGoogleUseCase {
    private final AuthUserRepository authUserRepository;
    private final TokenRepository tokenRepository;
    private final NatsJetStreamClient natBroker;
    private final String userLoginSubject;
    private final OAuthVerifier googleTokenVerifier;
    private final RegisterUseCase registerUseCase;
    private final MessagePublisher publisher;
    private final String redisInChannel;

    public LoginByGoogleUseCaseImpl(AuthUserRepository authUserRepository, TokenRepository tokenRepository, NatsJetStreamClient natBroker, String userLoginSubject, OAuthVerifier googleTokenVerifier, RegisterUseCase registerUseCase, MessagePublisher publisher, String redisInChannel) {
        this.authUserRepository = authUserRepository;
        this.tokenRepository = tokenRepository;
        this.natBroker = natBroker;
        this.userLoginSubject = userLoginSubject;
        this.googleTokenVerifier = googleTokenVerifier;
        this.registerUseCase = registerUseCase;
        this.publisher = publisher;
        this.redisInChannel = redisInChannel;
    }

    @Override
    public String login(String idToken) {
        OAuthUser googleAuthUser = null;
        try {
            googleAuthUser = googleTokenVerifier.verify(idToken);
        } catch (SecurityException e) {
            throw new UnauthorizedUserException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException("Google authentication error: " + e.getMessage());
        }

        var optAutUser = this.authUserRepository.getAuthUserByGoogleId(googleAuthUser.getProviderId());

        // register case
        if (optAutUser.isEmpty()) {
            Optional<AuthUser> emailUser = this.authUserRepository.getAuthUserByEmail(googleAuthUser.getEmail());
            if (emailUser.isPresent()) {
                throw new ConflictResourceException("Email already existed. Please login to link Google account");
            }

            // call to register usecase
            var req = new UserRegisterRequest();
            req.setEmail(googleAuthUser.getEmail());
            req.setGoogleId(googleAuthUser.getProviderId());
            req.setAvatar(googleAuthUser.getAvatar());
            req.setName(googleAuthUser.getName());
            req.setNickname(googleAuthUser.getName());

            registerUseCase.register(req);

            // get auth-user again
            optAutUser = this.authUserRepository.getAuthUserByGoogleId(googleAuthUser.getProviderId());
            if (optAutUser.isEmpty()) {
                throw new NotFoundUserException("User not found after created");
            }
        }

        // now we log user in here
        // we cannot use loginUsecase because we don't have password
        // TODO find a better solution
        var authUser = optAutUser.get();

        String strToken = JwtService.generateToken(authUser.getEmail(), authUser.getUserId(), authUser.getUUID(), authUser.getRole());

        Token token = new Token(authUser.getEmail(), strToken, JwtService.getExpirationDate());
        this.tokenRepository.addToken(token);

        try {
            AuthUserLoginEvent event = new AuthUserLoginEvent(authUser.getUserId(), authUser.getEmail());
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(event);
            natBroker.publish(this.userLoginSubject, json);
        } catch (Exception e) {
            throw new InternalServerException("Nat error: " + e.getMessage());
        }

        try {
            UserLoginEventData loginEventData = new UserLoginEventData(authUser.getUserId(), authUser.getEmail());
            int[] targetIds = {authUser.getUserId()};
            ExternalEvent event = new ExternalEvent("user_login", "auth-service", authUser.getUserId(), 0, targetIds, loginEventData);
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(event);
            publisher.publish(this.redisInChannel, json);
        } catch (Exception e) {
            throw new InternalServerException("Websocket Redis publish error: " + e.getMessage());
        }

        return strToken;
    }
} 