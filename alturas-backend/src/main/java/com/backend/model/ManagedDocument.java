package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "managed_documents")
public class ManagedDocument {

    @Id
    private String id;

    private String employeeId;
    private String documentType;
    private String examType;

    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String contentType;

    private String uploadedBy;
    private LocalDateTime uploadedAt;

    /*
     * Estados técnicos:
     * UPLOADED
     * ANALYZED
     * PENDING_MANUAL_REVIEW
     * ERROR
     */
    private String processingStatus;

    /*
     * Estados de revisión formal:
     * PENDING_REVIEW
     * APPROVED
     * REJECTED
     */
    private String reviewStatus;

    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    /*
     * Estados de notificación:
     * NOT_PENDING
     * SENT
     * FAILED
     * SKIPPED
     */
    private String notificationStatus;

    private LocalDateTime notifiedAt;

    private AreaCode areaCode;

    public ManagedDocument() {
    }

    public String getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getExamType() {
        return examType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public AreaCode getAreaCode() {
        return areaCode;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public void setAreaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
    }
}