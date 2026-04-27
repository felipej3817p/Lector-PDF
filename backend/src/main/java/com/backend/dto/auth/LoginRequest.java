package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    private String username;

    private String email;

    private String identifier;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String resolvePrincipal() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return "";
    }
}
