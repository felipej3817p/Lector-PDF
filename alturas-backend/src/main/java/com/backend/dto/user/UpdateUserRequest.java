package com.backend.dto.user;

import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.UserAreaAssignment;
import com.backend.model.UserRoleAssignment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UpdateUserRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @Size(min = 6, max = 120)
    private String password;

    private Set<Role> roles = new HashSet<>();
    private List<UserRoleAssignment> roleAssignments = new ArrayList<>();

    private Set<AreaCode> allowedAreas = new HashSet<>();
    private List<UserAreaAssignment> areaAssignments = new ArrayList<>();
    private boolean globalAreaAccess;

    private boolean enabled = true;
    private LocalDateTime accountStartDate;
    private LocalDateTime accountExpirationDate;

    public UpdateUserRequest() {
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

    public List<UserRoleAssignment> getRoleAssignments() { return roleAssignments; }
    public void setRoleAssignments(List<UserRoleAssignment> roleAssignments) { this.roleAssignments = roleAssignments; }

    public Set<AreaCode> getAllowedAreas() {
        return allowedAreas;
    }

    public void setAllowedAreas(Set<AreaCode> allowedAreas) {
        this.allowedAreas = allowedAreas;
    }

    public List<UserAreaAssignment> getAreaAssignments() { return areaAssignments; }
    public void setAreaAssignments(List<UserAreaAssignment> areaAssignments) { this.areaAssignments = areaAssignments; }

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

    public LocalDateTime getAccountStartDate() { return accountStartDate; }
    public void setAccountStartDate(LocalDateTime accountStartDate) { this.accountStartDate = accountStartDate; }

    public void setAccountExpirationDate(LocalDateTime accountExpirationDate) {
        this.accountExpirationDate = accountExpirationDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
