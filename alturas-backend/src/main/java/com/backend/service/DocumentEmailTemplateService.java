package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.SystemSettings;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import com.backend.repository.UserRepository;
import com.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentEmailTemplateService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final SystemSettingsService systemSettingsService;
    private final UserRepository userRepository;

    @Value("${app.email.default-cc:}")
    private String defaultCc;

    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    public DocumentEmailTemplateService(
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository,
            SystemSettingsService systemSettingsService,
            UserRepository userRepository) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
        this.systemSettingsService = systemSettingsService;
        this.userRepository = userRepository;
    }

    public Map<String, String> buildTemplate(String documentId) {
        ManagedDocument document = managedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el documento."));

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el analisis del documento."));

        Employee employee = resolveEmployee(document.getEmployeeId());
        String resultStatus = normalizeResultStatus(analysis.getResultStatus());
        String reviewStatus = normalizeReviewStatus(document.getReviewStatus());

        Map<String, String> template = new LinkedHashMap<>();

        if ("REJECTED".equals(reviewStatus)) {
            String operatorEmail = userRepository.findByUsername(document.getUploadedBy())
                    .map(User::getEmail)
                    .orElse("");

            template.put("to", operatorEmail);
            template.put("cc", "");
            template.put("subject", "Revisión de trabajador " + getFullName(employee) + " cédula " + getDocumentNumber(employee) + ": de trabajos en altura");
            template.put("body", buildOperatorEmailBody(document, employee));
        } else {
            template.put("to", resolveRecipient(employee));
            template.put("cc", resolveCc(employee));
            template.put("subject", buildSubject(resultStatus, reviewStatus));
            template.put("body", buildWorkerEmailBody(resultStatus, reviewStatus));
        }

        return template;
    }

    private String getFullName(Employee employee) {
        if (employee == null) return "Desconocido";
        String fullName = String.join(" ", 
                safe(employee.getFirstName()),
                safe(employee.getSecondName()),
                safe(employee.getFirstLastName()),
                safe(employee.getSecondLastName())).replaceAll("\\s+", " ").trim();
        return fullName.isEmpty() ? "Desconocido" : fullName;
    }

    private String getDocumentNumber(Employee employee) {
        if (employee == null) return "Desconocida";
        return safe(employee.getDocumentNumber()).isEmpty() ? "Desconocida" : safe(employee.getDocumentNumber());
    }

    private Employee resolveEmployee(String employeeId) {
        if (safe(employeeId).isBlank()) {
            return null;
        }

        return employeeRepository.findById(employeeId).orElse(null);
    }

    private String resolveRecipient(Employee employee) {
        return employee == null ? "" : safe(employee.getEmail());
    }

    private String resolveCc(Employee employee) {
        SystemSettings settings = systemSettingsService.getRuntimeSettings();

        String configuredCc = systemSettingsService.resolveWorkerCc(
                settings,
                employee != null ? employee.getAreaCode() : null);

        if (!safe(configuredCc).isBlank()) {
            return safe(configuredCc);
        }

        return safe(defaultCc);
    }

    private String buildSubject(String resultStatus, String reviewStatus) {
        if ("REJECTED".equals(reviewStatus)) {
            return "Resultado reentrenamiento - REVISIÓN";
        }

        return "Resultado reentrenamiento - " + resultLabel(resultStatus);
    }

    private String buildOperatorEmailBody(ManagedDocument document, Employee employee) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"UTF-8\"></head>");
        html.append(
                "<body style=\"margin:0; padding:24px; background:#ffffff; font-family:Arial, Helvetica, sans-serif; color:#111827;\">");
        html.append("<div style=\"max-width:680px;\">");
        html.append("<p style=\"margin:0 0 12px; font-size:14px; line-height:1.45;\">Buen dia,</p>");
        
        html.append("<p style=\"margin:0 0 12px; font-size:14px; line-height:1.55; color:#111827;\">");
        html.append("Por favor revisar el PDF del trabajador. Se ha solicitado una <strong>revisión</strong> del documento cargado para el trabajador <strong>")
            .append(getFullName(employee))
            .append("</strong> con cédula <strong>")
            .append(getDocumentNumber(employee))
            .append("</strong> de trabajos en altura.</p>");

        String comment = safe(document.getReviewComment());
        if (!comment.isEmpty()) {
            html.append("<p style=\"margin:0 0 12px; font-size:14px; line-height:1.55; color:#111827;\"><strong>Motivo de la revisión:</strong><br/>")
                .append(comment.replace("\n", "<br/>"))
                .append("</p>");
        }

        html.append("<p style=\"margin:0 0 14px; font-size:14px; line-height:1.55; color:#111827;\">")
            .append("Por favor ingrese a la plataforma, verifique el documento (si debe borrar el PDF, bórrelo y envíe el que ya está revisado) y realice el proceso correspondiente.</p>");

        if (frontendBaseUrl != null && !frontendBaseUrl.isEmpty()) {
            String appLink = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
            html.append("<p style=\"margin:14px 0;\">")
                .append("<a href=\"").append(appLink).append("\" style=\"background-color:#0d6efd; color:#ffffff; padding:10px 20px; text-decoration:none; border-radius:5px; font-weight:bold; display:inline-block;\">")
                .append("Ingresar al Aplicativo")
                .append("</a></p>");
        }

        html.append("<p style=\"margin:14px 0 0; font-size:14px; line-height:1.5; color:#111827;\">Cordialmente.</p>");
        html.append("</div></body></html>");

        return html.toString();
    }

    private String buildWorkerEmailBody(String resultStatus, String reviewStatus) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"UTF-8\"></head>");
        html.append(
                "<body style=\"margin:0; padding:24px; background:#ffffff; font-family:Arial, Helvetica, sans-serif; color:#111827;\">");
        html.append("<div style=\"max-width:680px;\">");
        html.append("<p style=\"margin:0 0 12px; font-size:14px; line-height:1.45;\">Buen dia,</p>");
        html.append(buildWorkerDecisionMessage(resultStatus, reviewStatus));
        html.append("<p style=\"margin:14px 0 0; font-size:14px; line-height:1.5; color:#111827;\">Cordialmente.</p>");
        html.append("</div></body></html>");

        return html.toString();
    }

    private String buildWorkerDecisionMessage(String resultStatus, String reviewStatus) {
        if ("REJECTED".equals(reviewStatus)) {
            return """
                    <p style="margin:0 0 12px; font-size:14px; line-height:1.55; color:#111827;">Se ha devuelto un documento cargado para su <strong>revisión</strong>.</p>
                    <p style="margin:0 0 14px; font-size:14px; line-height:1.55; color:#111827;">Por favor ingrese a la plataforma, verifique el documento y realice las correcciones necesarias antes de volver a enviarlo o eliminarlo.</p>
                    """;
        }

        if ("NO_APTO".equals(resultStatus)) {
            return """
                    <p style="margin:0 0 12px; font-size:14px; line-height:1.55; color:#111827;">Con base en el concepto emitido por el medico laboral, me permito informarle que usted se encuentra <strong>NO APTO TEMPORALMENTE</strong> para realizar trabajo en alturas.</p>
                    <p style="margin:0 0 14px; font-size:14px; line-height:1.55; color:#111827;">Por lo anterior, se le solicita asistir a control y tratamiento medico en su EPS, con el fin de realizar el seguimiento correspondiente y gestionar el levantamiento de la restriccion una vez se cuente con el concepto favorable.</p>
                    """;
        }

        if ("APTO".equals(resultStatus)) {
            return """
                    <p style="margin:0 0 12px; font-size:14px; line-height:1.55; color:#111827;">Con base en el concepto emitido por el medico laboral, me permito informarle que usted se encuentra <strong>APTO</strong> para realizar trabajo en alturas, dado que cumple con las condiciones de salud requeridas para el desarrollo de sus labores.</p>
                    <p style="margin:0 0 14px; font-size:14px; line-height:1.55; color:#111827;">Se recomienda fortalecer el autocuidado y asistir oportunamente a los controles medicos definidos, con el fin de preservar su estado de salud.</p>
                    """;
        }

        return "<p style=\"margin:0 0 14px; font-size:14px; line-height:1.55; color:#111827;\">Se informa que el concepto de aptitud para trabajo en alturas fue actualizado.</p>";
    }

    private String resultLabel(String resultStatus) {
        return switch (normalizeResultStatus(resultStatus)) {
            case "APTO" -> "APTO";
            case "NO_APTO" -> "NO APTO";
            default -> "PENDIENTE";
        };
    }

    private String normalizeResultStatus(String resultStatus) {
        String value = safe(resultStatus).toUpperCase();

        if ("APTO".equals(value)) {
            return "APTO";
        }

        if ("NO_APTO".equals(value) || "NO APTO".equals(value)) {
            return "NO_APTO";
        }

        return "NO_APTO";
    }

    private String normalizeReviewStatus(String reviewStatus) {
        String value = safe(reviewStatus).toUpperCase();

        if ("APPROVED".equals(value)) {
            return "APPROVED";
        }

        if ("REJECTED".equals(value)) {
            return "REJECTED";
        }

        return "PENDING_REVIEW";
    }

    private String safe(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }
}
