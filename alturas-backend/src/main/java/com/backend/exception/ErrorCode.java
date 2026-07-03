package com.backend.exception;

public enum ErrorCode {
    VALIDATION_ERROR("ALT-VAL-001", 400, "Error de validacion"),
    MISSING_PARAMETER("ALT-VAL-002", 400, "Parametro requerido faltante"),
    FILE_TOO_LARGE("ALT-FILE-001", 413, "Archivo demasiado grande"),
    INVALID_PDF_UPLOAD("ALT-PDF-001", 400, "Carga PDF invalida"),
    PDF_NOT_FOUND("ALT-PDF-002", 404, "Archivo PDF no encontrado"),
    PDF_TEXT_EXTRACTION_FAILED("ALT-PDF-003", 422, "No se pudo extraer texto del PDF"),
    DOCUMENT_NOT_FOUND("ALT-DOC-001", 404, "Documento no encontrado"),
    DOCUMENT_ANALYSIS_NOT_FOUND("ALT-DOC-002", 409, "Analisis documental no disponible"),
    DOCUMENT_RESULT_NOT_READY("ALT-DOC-003", 409, "Resultado documental pendiente"),
    EMPLOYEE_NOT_FOUND("ALT-EMP-001", 404, "Trabajador no encontrado"),
    USER_NOT_FOUND("ALT-USR-001", 404, "Usuario no encontrado"),
    AUTHENTICATION_FAILED("ALT-AUTH-001", 401, "Autenticacion fallida"),
    ACCESS_DENIED("ALT-AUTH-002", 403, "Acceso denegado"),
    TOKEN_INVALID("ALT-AUTH-003", 400, "Token invalido"),
    TOKEN_EXPIRED("ALT-AUTH-004", 400, "Token expirado"),
    MAIL_SEND_FAILED("ALT-MAIL-001", 502, "Fallo envio de correo"),
    CONFIGURATION_ERROR("ALT-CONF-001", 500, "Error de configuracion"),
    DATA_CONFLICT("ALT-DATA-001", 409, "Conflicto de datos"),
    INTERNAL_ERROR("ALT-SYS-001", 500, "Error interno");

    private final String code;
    private final int status;
    private final String title;

    ErrorCode(String code, int status, String title) {
        this.code = code;
        this.status = status;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
