package com.backend.dto.user;

import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.UserAreaAssignment;
import com.backend.model.UserRoleAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserDto {

    private String id;
    private String username;
    private String email;
    private Set<Role> roles = new HashSet<>();
    private List<UserRoleAssignment> roleAssignments = new ArrayList<>();
    private Set<Role> effectiveRoles = new HashSet<>();
    private Set<AreaCode> allowedAreas = new HashSet<>();
    private List<UserAreaAssignment> areaAssignments = new ArrayList<>();
    private Set<AreaCode> effectiveAllowedAreas = new HashSet<>();
    private boolean globalAreaAccess;
    private boolean enabled;
    private LocalDateTime accountStartDate;
    private LocalDateTime accountExpirationDate;
    private LocalDateTime createdAt;
    private String createdBy;
    private String updatedBy;
    private String enabledChangedBy;
    private LocalDateTime enabledChangedAt;
    
    private LocalDateTime lastLoginAt;
    private boolean mustChangePassword;

    public UserDto() {
    }

    public UserDto(
            String id,
            String username,
            String email,
            Set<Role> roles,
            List<UserRoleAssignment> roleAssignments,
            Set<Role> effectiveRoles,
            Set<AreaCode> allowedAreas,
            List<UserAreaAssignment> areaAssignments,
            Set<AreaCode> effectiveAllowedAreas,
            boolean globalAreaAccess,
            boolean enabled,
            LocalDateTime accountStartDate,
            LocalDateTime accountExpirationDate,
            LocalDateTime createdAt,
            String createdBy,
            String updatedBy,
            String enabledChangedBy,
            LocalDateTime enabledChangedAt,
            LocalDateTime lastLoginAt,
            boolean mustChangePassword
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.roleAssignments = roleAssignments;
        this.effectiveRoles = effectiveRoles;
        this.allowedAreas = allowedAreas;
        this.areaAssignments = areaAssignments;
        this.effectiveAllowedAreas = effectiveAllowedAreas;
        this.globalAreaAccess = globalAreaAccess;
        this.enabled = enabled;
        this.accountStartDate = accountStartDate;
        this.accountExpirationDate = accountExpirationDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.enabledChangedBy = enabledChangedBy;
        this.enabledChangedAt = enabledChangedAt;
        this.lastLoginAt = lastLoginAt;
        this.mustChangePassword = mustChangePassword;
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

    public List<UserAreaAssignment> getAreaAssignments() { return areaAssignments; }
    public void setAreaAssignments(List<UserAreaAssignment> areaAssignments) { this.areaAssignments = areaAssignments; }
    public Set<AreaCode> getEffectiveAllowedAreas() { return effectiveAllowedAreas; }
    public void setEffectiveAllowedAreas(Set<AreaCode> effectiveAllowedAreas) { this.effectiveAllowedAreas = effectiveAllowedAreas; }

    public boolean isGlobalAreaAccess() {
        return globalAreaAccess;
    }

    public void setGlobalAreaAccess(boolean globalAreaAccess) {
        this.globalAreaAccess = globalAreaAccess;
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

    public List<UserRoleAssignment> getRoleAssignments() { return roleAssignments; }
    public void setRoleAssignments(List<UserRoleAssignment> roleAssignments) { this.roleAssignments = roleAssignments; }
    public Set<Role> getEffectiveRoles() { return effectiveRoles; }
    public void setEffectiveRoles(Set<Role> effectiveRoles) { this.effectiveRoles = effectiveRoles; }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getAccountExpirationDate() {
        return accountExpirationDate;
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

    public void setAccountExpirationDate(LocalDateTime accountExpirationDate) {
        this.accountExpirationDate = accountExpirationDate;
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
