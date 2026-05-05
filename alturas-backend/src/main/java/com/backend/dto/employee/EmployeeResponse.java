package com.backend.dto.employee;

import com.backend.model.AreaCode;

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
    private AreaCode areaCode;
    private boolean active;

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
            AreaCode areaCode,
            boolean active
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
        this.areaCode = areaCode;
        this.active = active;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}