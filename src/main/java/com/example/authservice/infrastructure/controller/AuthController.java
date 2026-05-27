package com.example.authservice.infrastructure.controller;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.exception.InvalidParameterException;
import com.example.authservice.domain.exception.UnauthorizedUserException;
import com.example.authservice.domain.usecase.*;
import com.example.authservice.dto.*;
import com.example.authservice.infrastructure.service.JwtService;
import io.javalin.http.Handler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;

public class AuthController {
    public final Handler login;
    public final Handler loginByGoogle;
    public final Handler logout;
    public final Handler register;
    public final Handler updatePassword;
    public final Handler verify;

    public AuthController(Validator validator,
                          LoginUseCase loginUseCase,
                          LogoutUseCase logoutUseCase,
                          LoginByGoogleUseCase loginByGoogleUseCase,
                          RegisterUseCase registerUseCase,
                          UpdatePasswordUseCase updatePasswordUseCase,
                          VerifyTokenUseCase verifyTokenUseCase) {

        this.login = ctx -> {
            var request = ctx.bodyAsClass(LoginRequest.class);

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<LoginRequest> v : violations) {
                    sb.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
                }

                throw new InvalidParameterException(sb.toString());
            }

            String token = loginUseCase.login(request.getEmail(), request.getPassword());
            ctx.json(new ErrorResponse(new LoginResponse(token)));
        };

        this.logout = ctx -> {
            String token = ctx.header("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                logoutUseCase.logout(token);
            } else {
                throw new UnauthorizedUserException();
            }
            ctx.status(200);
        };

        this.loginByGoogle = ctx -> {
            var request = ctx.bodyAsClass(LoginByGoogleRequest.class);

            Set<ConstraintViolation<LoginByGoogleRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<LoginByGoogleRequest> v : violations) {
                    sb.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
                }

                throw new InvalidParameterException(sb.toString());
            }

            String token = loginByGoogleUseCase.login(request.getIdToken());
            ctx.json(new ErrorResponse(new LoginResponse(token)));
        };

        this.register = ctx -> {
            var request = ctx.bodyAsClass(UserRegisterRequest.class);

            Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<UserRegisterRequest> v : violations) {
                    sb.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
                }

                throw new InvalidParameterException(sb.toString());
            }

            AuthUser authUser = registerUseCase.register(request);
            ctx.json(new ErrorResponse(new RegisterResponse(authUser.getId().toString(), authUser.getEmail())));
        };

        this.verify = ctx -> {
            String token = ctx.header("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                boolean active = verifyTokenUseCase.verifyToken(token);
                ctx.json(new ErrorResponse(new VerifyResponse(active)));
            } else {
                throw new UnauthorizedUserException();
            }
        };

        this.updatePassword = ctx -> {
            String token = ctx.header("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new UnauthorizedUserException();
            }

            token = token.substring(7);
            var active = JwtService.validateToken(token);
            if (!active) {
                throw new UnauthorizedUserException();
            }

            String email = JwtService.getEmailFromToken(token);

            var request = ctx.bodyAsClass(UpdatePasswordRequest.class);
            updatePasswordUseCase.update(email, request.getOldPassword(), request.getPassword());
            ctx.status(204);
        };
    }
}
