package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(name = "users_username_idx")
    private String username;

    @Indexed(name = "users_email_idx")
    private String email;
    private String password;

    private Set<Role> roles = new HashSet<>();
    private List<UserRoleAssignment> roleAssignments = new ArrayList<>();

    /**
     * Áreas que puede ver/operar el usuario.
     * SUPER_ADMIN puede tener este set vacío.
     */
    private Set<AreaCode> allowedAreas = new HashSet<>();
    private List<UserAreaAssignment> areaAssignments = new ArrayList<>();
    private boolean globalAreaAccess;

    private boolean enabled = true;
    private LocalDateTime accountStartDate;
    private LocalDateTime accountExpirationDate;
    private String createdBy;
    private String updatedBy;
    private String enabledChangedBy;
    private LocalDateTime enabledChangedAt;
    
    private LocalDateTime lastLoginAt;
    private boolean mustChangePassword = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(
            String id,
            String username,
            String email,
            String password,
            Set<Role> roles,
            Set<AreaCode> allowedAreas,
            boolean enabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.allowedAreas = allowedAreas;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public List<UserRoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(List<UserRoleAssignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public Set<AreaCode> getAllowedAreas() {
        return allowedAreas;
    }

    public void setAllowedAreas(Set<AreaCode> allowedAreas) {
        this.allowedAreas = allowedAreas;
    }

    public List<UserAreaAssignment> getAreaAssignments() {
        return areaAssignments;
    }

    public void setAreaAssignments(List<UserAreaAssignment> areaAssignments) {
        this.areaAssignments = areaAssignments;
    }

    public boolean isGlobalAreaAccess() {
        return globalAreaAccess;
    }

    public void setGlobalAreaAccess(boolean globalAreaAccess) {
        this.globalAreaAccess = globalAreaAccess;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getAccountExpirationDate() {
        return accountExpirationDate;
    }

    public void setAccountExpirationDate(LocalDateTime accountExpirationDate) {
        this.accountExpirationDate = accountExpirationDate;
    }

    public LocalDateTime getAccountStartDate() { return accountStartDate; }
    public void setAccountStartDate(LocalDateTime accountStartDate) { this.accountStartDate = accountStartDate; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getEnabledChangedBy() { return enabledChangedBy; }
    public void setEnabledChangedBy(String enabledChangedBy) { this.enabledChangedBy = enabledChangedBy; }
    public LocalDateTime getEnabledChangedAt() { return enabledChangedAt; }
    public void setEnabledChangedAt(LocalDateTime enabledChangedAt) { this.enabledChangedAt = enabledChangedAt; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
