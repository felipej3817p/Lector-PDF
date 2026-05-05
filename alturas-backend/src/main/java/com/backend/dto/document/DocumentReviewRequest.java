package com.backend.dto.document;

import jakarta.validation.constraints.Size;

public class DocumentReviewRequest {

    @Size(max = 1000, message = "El comentario no puede superar 1000 caracteres.")
    private String comment;

    public DocumentReviewRequest() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}