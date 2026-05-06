package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "email_logs")
public class EmailLog {

    @Id
    private String id;

    private String documentId;
    private String batchId;
    private String employeeId;
    private String type;
    private String triggeredBy;
    private String retryOfEmailLogId;
    private LocalDateTime attemptedAt;
    private String employeeDocumentNumber;

    private String to;
    private String cc;
    private String subject;
    private String body;

    private String resultStatus;
    private String status; // SENT, FAILED, SKIPPED

    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public EmailLog() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeDocumentNumber() {
        return employeeDocumentNumber;
    }

    public void setEmployeeDocumentNumber(String employeeDocumentNumber) {
        this.employeeDocumentNumber = employeeDocumentNumber;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public String getRetryOfEmailLogId() { return retryOfEmailLogId; }
    public void setRetryOfEmailLogId(String retryOfEmailLogId) { this.retryOfEmailLogId = retryOfEmailLogId; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}
