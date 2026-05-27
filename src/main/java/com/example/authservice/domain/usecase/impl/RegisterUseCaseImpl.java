package com.example.authservice.domain.usecase.impl;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.entity.Role;
import com.example.authservice.domain.exception.ConflictResourceException;
import com.example.authservice.domain.exception.InternalServerException;
import com.example.authservice.domain.repository.AuthUserRepository;
import com.example.authservice.domain.usecase.RegisterUseCase;
import com.example.authservice.dto.UserRegisterRequest;
import com.example.authservice.infrastructure.service.nats.AuthUserRegisterEvent;
import com.example.authservice.infrastructure.service.nats.NatsJetStreamClient;
import com.example.authservice.infrastructure.service.user.UserServiceClient;
import com.example.authservice.infrastructure.service.user.user.CreateUserRequest;
import com.example.authservice.infrastructure.service.user.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import org.mindrot.jbcrypt.BCrypt;

import java.net.http.HttpConnectTimeoutException;

public class RegisterUseCaseImpl implements RegisterUseCase {
    private final AuthUserRepository authUserRepository;
    private final NatsJetStreamClient natBroker;
    private final String userRegisterSubject;

    public RegisterUseCaseImpl(AuthUserRepository authUserRepository, NatsJetStreamClient natBroker, String userRegisterSubject) {
        this.authUserRepository = authUserRepository;
        this.natBroker = natBroker;
        this.userRegisterSubject = userRegisterSubject;
    }

    @Override
    public AuthUser register(UserRegisterRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest(request.getName(), request.getGender(), request.getNickname(), request.getAvatar(), request.getBirthdate(), request.getEmail(), request.getRole());
        UserServiceClient userServiceClient = new UserServiceClient();
        UserResponse user;
        try {
            user = userServiceClient.addUser(createUserRequest);
        } catch (HttpConnectTimeoutException ex) {
            throw new RuntimeException("Timeout");
        } catch (HttpResponseException ex) {
            if (ex.getStatus() == HttpStatus.CONFLICT.getCode()) {
                throw new ConflictResourceException("Email already exists");
            }
            throw new RuntimeException("Register user: HTTP exception", ex);
        } catch (Exception e) {
            throw new RuntimeException("Register user: Call user service exception", e);
        }

        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        var authUser = new AuthUser(null, user.getId(), user.getUUID(), request.getEmail(), request.getGoogleId(), hashedPassword, Role.USER);
        this.authUserRepository.addAuthUser(authUser);

        try {
            AuthUserRegisterEvent event = new AuthUserRegisterEvent(authUser.getUserId(), request.getEmail());
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(event);
            natBroker.publish(this.userRegisterSubject, json);
        } catch (Exception e) {
            throw new InternalServerException("Nat error: " + e.getMessage());
        }

        return authUser;
    }
} 