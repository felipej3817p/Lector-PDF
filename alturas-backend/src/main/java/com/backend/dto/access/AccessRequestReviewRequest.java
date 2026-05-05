package com.backend.dto.access;

import jakarta.validation.constraints.Size;

public class AccessRequestReviewRequest {

    @Size(max = 500, message = "El comentario no puede superar 500 caracteres.")
    private String adminComment;

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }
}