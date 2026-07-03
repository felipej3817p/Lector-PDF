package com.backend.model;

import java.time.LocalDateTime;

public class UserAreaAssignment {

    private AreaCode areaCode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean enabled = true;
    private String createdBy;
    private LocalDateTime createdAt;

    public UserAreaAssignment() {
    }

    public UserAreaAssignment(AreaCode areaCode, LocalDateTime startDate, LocalDateTime endDate, boolean enabled) {
        this.areaCode = areaCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enabled = enabled;
    }

    public AreaCode getAreaCode() { return areaCode; }
    public void setAreaCode(AreaCode areaCode) { this.areaCode = areaCode; }
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
