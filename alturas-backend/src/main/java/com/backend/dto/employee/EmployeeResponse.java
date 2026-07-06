package com.backend.dto.employee;

import com.backend.model.AreaCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeResponse {

    private String id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String secondName;
    private String firstLastName;
    private String secondLastName;
    private String gender;
    private String birthDate;
    private String currentPosition;
    private String workArea;
    private String employer;
    private String arl;
    private String email;
    private String zone;
    private String educationalLevel;
    private AreaCode areaCode;
    private boolean active;
    private boolean currentlyActive;
    private LocalDateTime activeStartDate;
    private LocalDateTime activeExpirationDate;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime statusChangedAt;
    private String statusChangedBy;
    private LocalDate latestFechaEvaluacion;
    private String latestResultStatus;

    public EmployeeResponse() {
    }

    public EmployeeResponse(
            String id,
            String documentType,
            String documentNumber,
            String firstName,
            String secondName,
            String firstLastName,
            String secondLastName,
            String gender,
            String birthDate,
            String currentPosition,
            String workArea,
            String employer,
            String arl,
            String email,
            String zone,
            String educationalLevel,
            AreaCode areaCode,
            boolean active,
            boolean currentlyActive,
            LocalDateTime activeStartDate,
            LocalDateTime activeExpirationDate,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            LocalDateTime statusChangedAt,
            String statusChangedBy,
            LocalDate latestFechaEvaluacion,
            String latestResultStatus
    ) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.secondName = secondName;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.currentPosition = currentPosition;
        this.workArea = workArea;
        this.employer = employer;
        this.arl = arl;
        this.email = email;
        this.zone = zone;
        this.educationalLevel = educationalLevel;
        this.areaCode = areaCode;
        this.active = active;
        this.currentlyActive = currentlyActive;
        this.activeStartDate = activeStartDate;
        this.activeExpirationDate = activeExpirationDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.statusChangedAt = statusChangedAt;
        this.statusChangedBy = statusChangedBy;
        this.latestFechaEvaluacion = latestFechaEvaluacion;
        this.latestResultStatus = latestResultStatus;
    }

    public String getId() {
        return id;
    }

    public AreaCode getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getFirstLastName() {
        return firstLastName;
    }

    public void setFirstLastName(String firstLastName) {
        this.firstLastName = firstLastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }

    public String getWorkArea() {
        return workArea;
    }

    public void setWorkArea(String workArea) {
        this.workArea = workArea;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getArl() {
        return arl;
    }

    public void setArl(String arl) {
        this.arl = arl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getEducationalLevel() {
        return educationalLevel;
    }

    public void setEducationalLevel(String educationalLevel) {
        this.educationalLevel = educationalLevel;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getLatestFechaEvaluacion() {
        return latestFechaEvaluacion;
    }

    public LocalDate getLatestEvaluationDate() {
        return latestFechaEvaluacion;
    }

    public String getLatestResultStatus() {
        return latestResultStatus;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCurrentlyActive() { return currentlyActive; }
    public void setCurrentlyActive(boolean currentlyActive) { this.currentlyActive = currentlyActive; }
    public LocalDateTime getActiveStartDate() { return activeStartDate; }
    public void setActiveStartDate(LocalDateTime activeStartDate) { this.activeStartDate = activeStartDate; }
    public LocalDateTime getActiveExpirationDate() { return activeExpirationDate; }
    public void setActiveExpirationDate(LocalDateTime activeExpirationDate) { this.activeExpirationDate = activeExpirationDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getStatusChangedAt() { return statusChangedAt; }
    public void setStatusChangedAt(LocalDateTime statusChangedAt) { this.statusChangedAt = statusChangedAt; }
    public String getStatusChangedBy() { return statusChangedBy; }
    public void setStatusChangedBy(String statusChangedBy) { this.statusChangedBy = statusChangedBy; }

    public void setLatestFechaEvaluacion(LocalDate latestFechaEvaluacion) {
        this.latestFechaEvaluacion = latestFechaEvaluacion;
    }

    public void setLatestEvaluationDate(LocalDate latestEvaluationDate) {
        this.latestFechaEvaluacion = latestEvaluationDate;
    }

    public void setLatestResultStatus(String latestResultStatus) {
        this.latestResultStatus = latestResultStatus;
    }
}
