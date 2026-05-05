package com.backend.dto.employee;

import com.backend.model.AreaCode;

import java.time.LocalDateTime;

public class EmployeeDashboardResponse {

    private String id;
    private String documentType;
    private String documentNumber;
    private String fullName;
    private AreaCode areaCode;
    private String workArea;
    private String currentPosition;
    private String email;
    private String zone;
    private boolean active;

    private String lastDocumentId;
    private String lastOriginalFileName;
    private LocalDateTime lastUploadedAt;
    private String lastProcessingStatus;
    private String lastResultStatus;

    public EmployeeDashboardResponse() {
    }

    public EmployeeDashboardResponse(
            String id,
            String documentType,
            String documentNumber,
            String fullName,
            AreaCode areaCode,
            String workArea,
            String currentPosition,
            String email,
            String zone,
            boolean active,
            String lastDocumentId,
            String lastOriginalFileName,
            LocalDateTime lastUploadedAt,
            String lastProcessingStatus,
            String lastResultStatus
    ) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.fullName = fullName;
        this.areaCode = areaCode;
        this.workArea = workArea;
        this.currentPosition = currentPosition;
        this.email = email;
        this.zone = zone;
        this.active = active;
        this.lastDocumentId = lastDocumentId;
        this.lastOriginalFileName = lastOriginalFileName;
        this.lastUploadedAt = lastUploadedAt;
        this.lastProcessingStatus = lastProcessingStatus;
        this.lastResultStatus = lastResultStatus;
    }

    public String getId() {
        return id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public AreaCode getAreaCode() {
        return areaCode;
    }

    public String getWorkArea() {
        return workArea;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public String getEmail() {
        return email;
    }

    public String getZone() {
        return zone;
    }

    public boolean isActive() {
        return active;
    }

    public String getLastDocumentId() {
        return lastDocumentId;
    }

    public String getLastOriginalFileName() {
        return lastOriginalFileName;
    }

    public LocalDateTime getLastUploadedAt() {
        return lastUploadedAt;
    }

    public String getLastProcessingStatus() {
        return lastProcessingStatus;
    }

    public String getLastResultStatus() {
        return lastResultStatus;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setAreaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
    }

    public void setWorkArea(String workArea) {
        this.workArea = workArea;
    }

    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLastDocumentId(String lastDocumentId) {
        this.lastDocumentId = lastDocumentId;
    }

    public void setLastOriginalFileName(String lastOriginalFileName) {
        this.lastOriginalFileName = lastOriginalFileName;
    }

    public void setLastUploadedAt(LocalDateTime lastUploadedAt) {
        this.lastUploadedAt = lastUploadedAt;
    }

    public void setLastProcessingStatus(String lastProcessingStatus) {
        this.lastProcessingStatus = lastProcessingStatus;
    }

    public void setLastResultStatus(String lastResultStatus) {
        this.lastResultStatus = lastResultStatus;
    }
}