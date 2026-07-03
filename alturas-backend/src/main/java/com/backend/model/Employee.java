package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String documentType;
    @Indexed(name = "employees_document_number_idx")
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

    /**
     * Area operativa principal del trabajador.
     */
    @Indexed(name = "employees_area_code_idx")
    private AreaCode areaCode;

    private boolean active = true;
    private LocalDateTime activeStartDate;
    private LocalDateTime activeExpirationDate;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime statusChangedAt;
    private String statusChangedBy;

    private java.time.LocalDate latestFechaConcepto;
    private String latestResultStatus;

    public Employee() {
    }

    public String getId() {
        return id;
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

    public AreaCode getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean started = activeStartDate == null || !now.isBefore(activeStartDate);
        boolean notExpired = activeExpirationDate == null || now.isBefore(activeExpirationDate);
        return active && started && notExpired;
    }

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

    public java.time.LocalDate getLatestFechaConcepto() {
        return latestFechaConcepto;
    }

    public java.time.LocalDate getLatestFechaEvaluacion() {
        return latestFechaConcepto;
    }

    public java.time.LocalDate getLatestEvaluationDate() {
        return latestFechaConcepto;
    }

    public void setLatestFechaConcepto(java.time.LocalDate latestFechaConcepto) {
        this.latestFechaConcepto = latestFechaConcepto;
    }

    public void setLatestFechaEvaluacion(java.time.LocalDate latestFechaEvaluacion) {
        this.latestFechaConcepto = latestFechaEvaluacion;
    }

    public void setLatestEvaluationDate(java.time.LocalDate latestEvaluationDate) {
        this.latestFechaConcepto = latestEvaluationDate;
    }

    public String getLatestResultStatus() {
        return latestResultStatus;
    }

    public void setLatestResultStatus(String latestResultStatus) {
        this.latestResultStatus = latestResultStatus;
    }
}
