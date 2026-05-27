package com.example.authservice.domain.repository;

import com.example.authservice.domain.entity.AuthUser;

import java.util.Optional;

public interface AuthUserRepository {
    void addAuthUser(AuthUser authUser);

    Optional<AuthUser> getAuthUserById(Integer id);

    Optional<AuthUser> getAuthUserByEmail(String email);

    Optional<AuthUser> getAuthUserByGoogleId(String googleId);

    boolean deleteAuthUser(Integer id);

    boolean updateAuthUser(AuthUser authUser);
}
