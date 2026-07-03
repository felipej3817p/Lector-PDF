package com.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex, HttpServletRequest request) {
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(classifyIllegalArgument(ex.getMessage()), ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ErrorCode.MISSING_PARAMETER,
                "Falta el parametro requerido: " + ex.getParameterName(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> buildValidationMessage(error.getField(), error.getDefaultMessage()))
                .orElse("La solicitud no cumple las validaciones requeridas.");

        return buildResponse(ErrorCode.VALIDATION_ERROR, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> buildValidationMessage(
                        lastPathSegment(violation.getPropertyPath().toString()),
                        violation.getMessage()
                ))
                .orElse("Revisa la informacion ingresada. Hay campos obligatorios o con formato invalido.");

        return buildResponse(ErrorCode.VALIDATION_ERROR, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ErrorCode.VALIDATION_ERROR,
                "No pudimos interpretar uno de los datos enviados. Revisa que fechas, roles y zonas tengan valores validos.",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ErrorCode.FILE_TOO_LARGE,
                "El archivo o la carga completa supera el tamano permitido.",
                request
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipart(
            MultipartException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ErrorCode.INVALID_PDF_UPLOAD,
                "El servidor no recibio completa la carga del PDF. Puede pasar por tamano, conexion inestable o porque el navegador corto la subida. Vuelve a intentar con ese PDF; los demas archivos correctos pueden continuar.",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(ErrorCode.ACCESS_DENIED, "No tienes permisos para realizar esta accion.", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return buildResponse(classifyIllegalState(ex.getMessage()), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();

        return buildResponse(
                ErrorCode.INTERNAL_ERROR,
                "Error interno no controlado. Revisa logs con codigo "
                        + ErrorCode.INTERNAL_ERROR.getCode()
                        + ". Detalle: "
                        + ex.getClass().getSimpleName(),
                request
        );
    }

    private ResponseEntity<ApiError> buildResponse(ErrorCode code, String message, HttpServletRequest request) {
        int status = code.getStatus();
        ApiError body = new ApiError(
                code.getCode(),
                status,
                code.getTitle(),
                message,
                request != null ? request.getRequestURI() : "",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatusCode.valueOf(status)).body(body);
    }

    private ErrorCode classifyIllegalArgument(String message) {
        String normalized = normalize(message);

        if (normalized.contains("permiso")) {
            return ErrorCode.ACCESS_DENIED;
        }
        if (normalized.contains("inhabilitado")
                || normalized.contains("inactivo")
                || normalized.contains("vigencia")) {
            return ErrorCode.AUTHENTICATION_FAILED;
        }
        if (normalized.contains("credencial") || normalized.contains("contrasena invalida")) {
            return ErrorCode.AUTHENTICATION_FAILED;
        }
        if (normalized.contains("token invalido") || normalized.contains("enlace de recuperacion no es valido")) {
            return ErrorCode.TOKEN_INVALID;
        }
        if (normalized.contains("expir")) {
            return ErrorCode.TOKEN_EXPIRED;
        }
        if (normalized.contains("pdf")
                && (normalized.contains("solo") || normalized.contains("adjuntar")
                || normalized.contains("vacia") || normalized.contains("vacio"))) {
            return ErrorCode.INVALID_PDF_UPLOAD;
        }
        if (normalized.contains("archivo fisico")
                || normalized.contains("archivo pdf")
                || normalized.contains("ruta de archivo")) {
            return ErrorCode.PDF_NOT_FOUND;
        }
        if (normalized.contains("analisis")) {
            return ErrorCode.DOCUMENT_ANALYSIS_NOT_FOUND;
        }
        if (normalized.contains("documento no encontrado")
                || normalized.contains("documento debe tener")
                || normalized.contains("no se encontro el documento")) {
            return ErrorCode.DOCUMENT_NOT_FOUND;
        }
        if (normalized.contains("no se puede eliminar")
                || normalized.contains("evaluaciones o pdfs asociados")) {
            return ErrorCode.DATA_CONFLICT;
        }
        if (normalized.contains("trabajador") || normalized.contains("persona")) {
            return ErrorCode.EMPLOYEE_NOT_FOUND;
        }
        if (normalized.contains("usuario")) {
            return ErrorCode.USER_NOT_FOUND;
        }
        if (normalized.contains("ya existe") || normalized.contains("duplic")) {
            return ErrorCode.DATA_CONFLICT;
        }
        if (normalized.contains("resultado pendiente") || normalized.contains("apto o no_apto")) {
            return ErrorCode.DOCUMENT_RESULT_NOT_READY;
        }

        return ErrorCode.VALIDATION_ERROR;
    }

    private ErrorCode classifyIllegalState(String message) {
        String normalized = normalize(message);

        if (normalized.contains("correo")) {
            return ErrorCode.MAIL_SEND_FAILED;
        }
        if (normalized.contains("token")) {
            return ErrorCode.CONFIGURATION_ERROR;
        }

        return ErrorCode.INTERNAL_ERROR;
    }

    private String buildValidationMessage(String field, String defaultMessage) {
        String label = humanFieldName(field);
        String normalizedMessage = normalize(defaultMessage);

        if (normalizedMessage.contains("must not be blank")
                || normalizedMessage.contains("no debe estar vacio")
                || normalizedMessage.contains("no debe estar en blanco")
                || normalizedMessage.contains("blank")) {
            return "Completa el campo " + label + ".";
        }

        if (normalizedMessage.contains("email")
                || normalizedMessage.contains("correo")
                || normalizedMessage.contains("well-formed")) {
            return "Ingresa un correo valido en el campo " + label + ".";
        }

        if (normalizedMessage.contains("null")) {
            return "Selecciona un valor para " + label + ".";
        }

        return "Revisa el campo " + label + ". " + safe(defaultMessage);
    }

    private String humanFieldName(String field) {
        String normalized = field == null ? "" : field.trim();

        return switch (normalized) {
            case "documentType" -> "tipo de documento";
            case "documentNumber" -> "numero de documento";
            case "firstName" -> "primer nombre";
            case "secondName" -> "segundo nombre";
            case "firstLastName" -> "primer apellido";
            case "secondLastName" -> "segundo apellido";
            case "gender" -> "genero";
            case "birthDate" -> "fecha de nacimiento";
            case "currentPosition" -> "cargo";
            case "workArea" -> "area o dependencia";
            case "areaCode" -> "zona o regional";
            case "zone" -> "zona";
            case "employer" -> "empleador";
            case "arl" -> "ARL";
            case "email" -> "correo";
            case "username" -> "usuario";
            case "password" -> "contrasena";
            case "roles", "roleAssignments" -> "roles";
            case "allowedAreas", "areaAssignments" -> "zonas permitidas";
            case "accountStartDate" -> "fecha inicial de acceso";
            case "accountExpirationDate" -> "fecha final de acceso";
            case "startDate" -> "fecha inicial";
            case "endDate" -> "fecha final";
            default -> normalized.isBlank() ? "solicitado" : normalized;
        };
    }

    private String lastPathSegment(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase()
                        .replace('á', 'a')
                        .replace('é', 'e')
                        .replace('í', 'i')
                        .replace('ó', 'o')
                        .replace('ú', 'u')
                        .replace('ñ', 'n');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
