package com.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "access_requests")
public class AccessRequest {

    @Id
    private String id;

    private String requestedByUserId;
    private String requestedByUsername;

    private AreaCode requestedArea;
    private String reason;

    private String status; // PENDING, APPROVED, REJECTED

    private String reviewedByUserId;
    private String reviewedByUsername;
    private String adminComment;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public AccessRequest() {
    }

    public String getId() {
        return id;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public AreaCode getRequestedArea() {
        return requestedArea;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewedByUserId() {
        return reviewedByUserId;
    }

    public String getReviewedByUsername() {
        return reviewedByUsername;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRequestedByUserId(String requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public void setRequestedArea(AreaCode requestedArea) {
        this.requestedArea = requestedArea;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReviewedByUserId(String reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
    }

    public void setReviewedByUsername(String reviewedByUsername) {
        this.reviewedByUsername = reviewedByUsername;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}