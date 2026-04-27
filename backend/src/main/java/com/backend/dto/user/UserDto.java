package com.backend.dto.user;

import com.backend.model.AreaCode;
import com.backend.model.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class UserDto {

    private String id;
    private String username;
    private String email;
    private Set<Role> roles = new HashSet<>();
    private Set<AreaCode> allowedAreas = new HashSet<>();
    private boolean enabled;
    private LocalDateTime createdAt;

    public UserDto() {
    }

    public UserDto(
            String id,
            String username,
            String email,
            Set<Role> roles,
            Set<AreaCode> allowedAreas,
            boolean enabled,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.allowedAreas = allowedAreas;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<AreaCode> getAllowedAreas() {
        return allowedAreas;
    }

    public void setAllowedAreas(Set<AreaCode> allowedAreas) {
        this.allowedAreas = allowedAreas;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}