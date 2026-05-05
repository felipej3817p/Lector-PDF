package com.backend.dto.access;

import com.backend.model.AreaCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AccessRequestCreateRequest {

    @NotNull(message = "El área solicitada es obligatoria.")
    private AreaCode requestedArea;

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
    private String reason;

    public AreaCode getRequestedArea() {
        return requestedArea;
    }

    public void setRequestedArea(AreaCode requestedArea) {
        this.requestedArea = requestedArea;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}