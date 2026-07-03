package com.backend.model;

import java.time.LocalDateTime;

public class UserRoleAssignment {

    private Role role;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean enabled = true;
    private String createdBy;
    private LocalDateTime createdAt;

    public UserRoleAssignment() {
    }

    public UserRoleAssignment(Role role, LocalDateTime startDate, LocalDateTime endDate, boolean enabled) {
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enabled = enabled;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
