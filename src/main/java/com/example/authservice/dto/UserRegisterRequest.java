package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class UserRegisterRequest {
    @Pattern(regexp = "USER|ADMIN", message = "Role must be 'USER' or 'ADMIN'")
    private String role = "USER";

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String googleId;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    private String gender;
    private String nickname;
    private String avatar;
    private LocalDate birthdate;

    public UserRegisterRequest() {

    }

    public UserRegisterRequest(String email, String googleId, String password, String name, String gender, String nickname, String avatar, LocalDate birthdate) {
        this.email = email;
        this.googleId = googleId;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.nickname = nickname;
        this.avatar = avatar;
        this.birthdate = birthdate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role == null || role.isBlank()) {
            this.role = "USER";
            return;
        }

        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }
} 