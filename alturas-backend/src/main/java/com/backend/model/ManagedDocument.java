package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "managed_documents")
@CompoundIndexes({
        @CompoundIndex(name = "managed_documents_employee_uploaded_idx", def = "{ 'employeeId': 1, 'uploadedAt': -1 }"),
        @CompoundIndex(name = "managed_documents_batch_uploaded_idx", def = "{ 'batchId': 1, 'uploadedAt': -1 }"),
        @CompoundIndex(name = "managed_documents_area_uploaded_idx", def = "{ 'areaCode': 1, 'uploadedAt': -1 }")
})
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
     * Fecha real del concepto/examen dentro del PDF.
     * No reemplaza uploadedAt: uploadedAt es auditoría de carga; fechaConcepto es la fecha usada en reportes.
     */
    private LocalDate fechaConcepto;

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
    private String notificationError;

    private LocalDateTime notifiedAt;

    private AreaCode areaCode;
    private String batchId;
    private String batchCode;
    private Boolean historical;

    @Transient
    private String resultStatus;

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

    public LocalDate getFechaConcepto() {
        return fechaConcepto;
    }

    public LocalDate getFechaEvaluacion() {
        return fechaConcepto;
    }

    public LocalDate getEvaluationDate() {
        return fechaConcepto;
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

    public String getNotificationError() {
        return notificationError;
    }

    public boolean isHistorical() {
        if (Boolean.TRUE.equals(historical)) {
            return true;
        }

        String status = processingStatus != null ? processingStatus : "";
        String comment = reviewComment != null ? reviewComment : "";
        String notification = notificationError != null ? notificationError : "";

        return "STORED".equalsIgnoreCase(status)
                || comment.toLowerCase().contains("carga historica")
                || notification.toLowerCase().contains("carga historica");
    }

    public Boolean getHistorical() {
        return historical;
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

    public void setFechaConcepto(LocalDate fechaConcepto) {
        this.fechaConcepto = fechaConcepto;
    }

    public void setFechaEvaluacion(LocalDate fechaEvaluacion) {
        this.fechaConcepto = fechaEvaluacion;
    }

    public void setEvaluationDate(LocalDate evaluationDate) {
        this.fechaConcepto = evaluationDate;
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

    public void setNotificationError(String notificationError) {
        this.notificationError = notificationError;
    }

    public void setHistorical(Boolean historical) {
        this.historical = historical;
    }

    public void setAreaCode(AreaCode areaCode) { this.areaCode = areaCode; }
    public String getBatchId(){return batchId;} public void setBatchId(String batchId){this.batchId=batchId;}
    public String getBatchCode(){return batchCode;} public void setBatchCode(String batchCode){this.batchCode=batchCode;}
    public String getResultStatus(){return resultStatus;} public void setResultStatus(String resultStatus){this.resultStatus=resultStatus;}
}
