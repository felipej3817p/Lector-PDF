package com.backend.dto.auth;

import com.backend.model.AreaCode;
import com.backend.model.Role;

import java.util.HashSet;
import java.util.Set;

public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private Set<Role> roles = new HashSet<>();
    private Set<AreaCode> allowedAreas = new HashSet<>();
    private boolean globalAreaAccess;
    private java.time.LocalDateTime lastLoginAt;
    private boolean mustChangePassword;

    public AuthResponse() {
    }

    public AuthResponse(
            String token,
            String username,
            String email,
            Set<Role> roles,
            Set<AreaCode> allowedAreas,
            boolean globalAreaAccess,
            java.time.LocalDateTime lastLoginAt,
            boolean mustChangePassword
    ) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.allowedAreas = allowedAreas;
        this.globalAreaAccess = globalAreaAccess;
        this.lastLoginAt = lastLoginAt;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<AreaCode> getAllowedAreas() {
        return allowedAreas;
    }

    public void setAllowedAreas(Set<AreaCode> allowedAreas) {
        this.allowedAreas = allowedAreas;
    }

    public boolean isGlobalAreaAccess() {
        return globalAreaAccess;
    }

    public void setGlobalAreaAccess(boolean globalAreaAccess) {
        this.globalAreaAccess = globalAreaAccess;
    }

    public java.time.LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(java.time.LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
