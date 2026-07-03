package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.DocumentBatch;
import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.SystemSettings;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.DocumentBatchRepository;
import com.backend.repository.EmailLogRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailSendService {

    private static final String WORKER_NOTIFICATION = "WORKER_NOTIFICATION";
    private static final String WORKER_NOTIFICATION_RESEND = "WORKER_NOTIFICATION_RESEND";
    private static final String APPROVER_BATCH_NOTIFICATION = "APPROVER_BATCH_NOTIFICATION";

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final JavaMailSender mailSender;
    private final DocumentEmailTemplateService documentEmailTemplateService;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailLogRepository emailLogRepository;
    private final DocumentBatchRepository documentBatchRepository;
    private final SystemSettingsService systemSettingsService;

    @Value("${app.email.auto-send-enabled:true}")
    private boolean autoSendEnabled;

    @Value("${spring.mail.username:}")
    private String from;

    public EmailSendService(
            JavaMailSender mailSender,
            DocumentEmailTemplateService documentEmailTemplateService,
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository,
            EmailLogRepository emailLogRepository,
            DocumentBatchRepository documentBatchRepository,
            SystemSettingsService systemSettingsService) {
        this.mailSender = mailSender;
        this.documentEmailTemplateService = documentEmailTemplateService;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
        this.emailLogRepository = emailLogRepository;
        this.documentBatchRepository = documentBatchRepository;
        this.systemSettingsService = systemSettingsService;
    }

    /*
     * Segundo envío:
     * Se usa cuando el aprobador aprueba o rechaza una evaluación.
     * El cuerpo del correo lo arma DocumentEmailTemplateService.
     */
    public EmailLog sendAnalysisEmailIfEnabled(String documentId) {
        if (!autoSendEnabled) {
            return saveSkippedLog(
                    documentId,
                    WORKER_NOTIFICATION,
                    "El envío automático de correos está deshabilitado.");
        }

        return sendAnalysisEmailInternal(documentId, false);
    }

    public EmailLog sendAnalysisEmail(String documentId) {
        return sendAnalysisEmailInternal(documentId, false);
    }

    public EmailLog resendAnalysisEmail(String documentId) {
        return sendAnalysisEmailInternal(documentId, true);
    }

    private EmailLog sendAnalysisEmailInternal(String documentId, boolean manualResend) {
        ManagedDocument document = managedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento."));

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el análisis del documento."));

        String logType = manualResend ? WORKER_NOTIFICATION_RESEND : WORKER_NOTIFICATION;

        if (safe(document.getEmployeeId()).isBlank()) {
            return saveSkippedLog(
                    documentId,
                    logType,
                    "El documento no tiene trabajador asociado.");
        }

        Employee employee = employeeRepository.findById(document.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el trabajador asociado."));

        String resultStatus = normalizeResultStatus(analysis.getResultStatus());
        String reviewStatus = safe(document.getReviewStatus()).toUpperCase();

        if (!"APPROVED".equals(reviewStatus) && !"REJECTED".equals(reviewStatus)) {
            return saveSkippedLog(
                    document,
                    employee,
                    analysis,
                    logType,
                    "El documento aún no ha sido aprobado o rechazado.");
        }

        if (!"APTO".equals(resultStatus) && !"NO_APTO".equals(resultStatus)) {
            return saveSkippedLog(
                    document,
                    employee,
                    analysis,
                    logType,
                    "Resultado pendiente o no concluyente. No se envía correo al trabajador.");
        }

        if (safe(employee.getEmail()).isBlank()) {
            return saveSkippedLog(
                    document,
                    employee,
                    analysis,
                    logType,
                    "El trabajador " + valueOrDash(buildEmployeeName(employee)) +
                            " (" + valueOrDash(employee.getDocumentNumber()) +
                            ") no tiene correo registrado en la base de datos. Actualiza la ficha del trabajador.");
        }

        if (!manualResend && emailLogRepository.existsByDocumentIdAndResultStatusAndStatus(
                documentId,
                resultStatus,
                STATUS_SENT)) {
            return saveSkippedLog(
                    document,
                    employee,
                    analysis,
                    WORKER_NOTIFICATION,
                    "Ya existe un correo enviado para este documento y resultado.");
        }

        Map<String, String> template = documentEmailTemplateService.buildTemplate(documentId);

        String to = safe(template.get("to"));
        String cc = safe(template.get("cc"));
        String subject = safe(template.get("subject"));
        String body = safe(template.get("body"));

        if (to.isBlank()) {
            return saveSkippedLog(
                    document,
                    employee,
                    analysis,
                    logType,
                    "No se pudo resolver destinatario del trabajador.");
        }

        EmailLog log = buildBaseLog(document, employee, analysis);
        log.setType(logType);
        log.setTriggeredBy(manualResend ? "MANUAL" : "AUTO");
        log.setAttemptedAt(LocalDateTime.now());
        log.setTo(to);
        log.setCc(cc);
        log.setSubject(subject);
        log.setBody(body);

        try {
            sendHtmlEmail(to, cc, subject, body);

            log.setStatus(STATUS_SENT);
            log.setSentAt(LocalDateTime.now());
            log.setErrorMessage("");

            EmailLog savedLog = emailLogRepository.save(log);
            updateDocumentNotificationStatus(document, STATUS_SENT, "");

            return savedLog;
        } catch (MailException | MessagingException ex) {
            String readableError = buildReadableMailError(ex);

            boolean partiallySent = wasPartiallySent(ex);
            log.setStatus(partiallySent ? STATUS_SENT : STATUS_FAILED);
            log.setErrorMessage(readableError);

            if (partiallySent) {
                log.setSentAt(LocalDateTime.now());
            }

            EmailLog savedLog = emailLogRepository.save(log);
            updateDocumentNotificationStatus(document, savedLog.getStatus(), readableError);

            return savedLog;
        }
    }

    /*
     * Primer envío:
     * Se usa cuando se cargan PDFs.
     * Este correo va al aprobador.
     * Lleva tabla tipo Excel + botón para abrir el panel de revisión.
     */
    public EmailLog sendBatchSummaryEmail(String batchId) {
        DocumentBatch batch = documentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la carga."));

        EmailLog log = new EmailLog();
        log.setBatchId(batchId);
        log.setType(APPROVER_BATCH_NOTIFICATION);
        log.setTriggeredBy("AUTO");
        log.setAttemptedAt(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());

        SystemSettings settings = systemSettingsService.getRuntimeSettings();

        if (!settings.isAutoSendApproverEmail()) {
            return finishApproverLog(
                    batch,
                    log,
                    STATUS_SKIPPED,
                    "El envío automático al aprobador está deshabilitado.");
        }

        String approverTo = systemSettingsService.resolveApproverEmails(settings);
        String approverCc = systemSettingsService.resolveApproverCc(settings);

        if (approverTo.isBlank()) {
            return finishApproverLog(
                    batch,
                    log,
                    STATUS_SKIPPED,
                    "No hay correo de aprobador configurado en Configuración > Correos.");
        }

        List<ManagedDocument> documents = managedDocumentRepository.findByBatchIdOrderByUploadedAtDesc(batchId)
                .stream()
                .filter(document -> !safe(document.getEmployeeId()).isBlank())
                .toList();

        if (documents.isEmpty()) {
            return finishApproverLog(
                    batch,
                    log,
                    STATUS_SKIPPED,
                    "La carga no tiene documentos válidos para revisión.");
        }

        String subject = buildApproverSubject(documents.size());
        String body = buildApproverBody(
                documents,
                systemSettingsService.resolveFrontendBaseUrl(settings));

        log.setTo(approverTo);
        log.setCc(approverCc);
        log.setSubject(subject);
        log.setBody(body);

        try {
            sendHtmlEmail(approverTo, approverCc, subject, body);

            log.setStatus(STATUS_SENT);
            log.setSentAt(LocalDateTime.now());
            log.setErrorMessage("");

            EmailLog savedLog = emailLogRepository.save(log);

            batch.setApproverNotificationStatus(STATUS_SENT);
            batch.setApproverNotifiedAt(LocalDateTime.now());
            batch.setApproverNotificationError("");
            documentBatchRepository.save(batch);

            return savedLog;
        } catch (MailException | MessagingException ex) {
            String readableError = buildReadableMailError(ex);

            boolean partiallySent = wasPartiallySent(ex);
            log.setStatus(partiallySent ? STATUS_SENT : STATUS_FAILED);
            log.setErrorMessage(readableError);

            if (partiallySent) {
                log.setSentAt(LocalDateTime.now());
            }

            EmailLog savedLog = emailLogRepository.save(log);

            batch.setApproverNotificationStatus(savedLog.getStatus());
            batch.setApproverNotificationError(readableError);

            if (partiallySent) {
                batch.setApproverNotifiedAt(savedLog.getSentAt());
            }

            documentBatchRepository.save(batch);

            return savedLog;
        }
    }

    private String buildApproverSubject(int documentCount) {
        return documentCount == 1
                ? "Concepto de aptitud pendiente de revisión"
                : "Conceptos de aptitud pendientes de revisión";
    }

    private String buildApproverBody(List<ManagedDocument> documents, String frontendBaseUrl) {
        return buildCompactApproverBody(documents, frontendBaseUrl);
    }

    private String buildReviewUrl(String frontendBaseUrl) {
        String baseUrl = safe(frontendBaseUrl);

        if (baseUrl.isBlank()) {
            return "/review";
        }

        return baseUrl.replaceAll("/+$", "") + "/review";
    }

    private String buildCompactApproverBody(List<ManagedDocument> documents, String frontendBaseUrl) {
        String reviewUrl = buildReviewUrl(frontendBaseUrl);
        int aptCount = 0;
        int notAptCount = 0;
        int pendingCount = 0;
        List<String> workersWithoutEmail = new java.util.ArrayList<>();

        for (ManagedDocument document : documents) {
            Employee employee = resolveEmployee(document.getEmployeeId());
            DocumentAnalysis analysis = resolveAnalysis(document.getId());
            String result = analysis != null ? normalizeResultStatus(analysis.getResultStatus()) : "PENDIENTE";

            if ("APTO".equals(result)) {
                aptCount++;
            } else if ("NO_APTO".equals(result)) {
                notAptCount++;
            } else {
                pendingCount++;
            }

            if (employee != null && safe(employee.getEmail()).isBlank()) {
                workersWithoutEmail.add(valueOrDash(buildEmployeeName(employee)) +
                        " (" + valueOrDash(employee.getDocumentNumber()) + ")");
            }

        }

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html>");
        html.append("<body style=\"margin:0; padding:0; background:#f3f4f6; font-family:Arial, Helvetica, sans-serif; color:#111827;\">");
        html.append("<div style=\"max-width:640px; margin:0 auto; padding:18px;\">");
        html.append("<div style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:10px; overflow:hidden;\">");
        html.append("<div style=\"background:#0f172a; padding:18px 22px; color:#ffffff;\">");
        html.append("<p style=\"margin:0 0 4px; font-size:12px; letter-spacing:.06em; text-transform:uppercase; color:#a7f3d0; font-weight:700;\">SST Alturas</p>");
        html.append("<h1 style=\"margin:0; font-size:19px; line-height:1.25;\">Hay conceptos pendientes por revisar</h1>");
        html.append("</div>");
        html.append("<div style=\"padding:18px 22px;\">");
        html.append("<p style=\"margin:0 0 12px; font-size:14px; line-height:1.45;\">Buen dia,</p>");
        html.append("<p style=\"margin:0 0 14px; font-size:14px; line-height:1.5;\">Se cargaron conceptos medicos de aptitud para trabajo en alturas. Ingrese a la aplicacion para revisar, aprobar o rechazar los documentos pendientes.</p>");
        html.append("<p style=\"margin:0 0 14px; font-size:13px; line-height:1.5; color:#374151;\">");
        html.append("<strong>Total:</strong> ").append(documents.size())
                .append(" &nbsp; <strong>Aptos:</strong> ").append(aptCount)
                .append(" &nbsp; <strong>No aptos:</strong> ").append(notAptCount)
                .append(" &nbsp; <strong>Pendientes:</strong> ").append(pendingCount);
        html.append("</p>");
        if (!workersWithoutEmail.isEmpty()) {
            html.append("<div style=\"margin:0 0 14px; padding:12px 14px; border:1px solid #fde68a; border-radius:8px; background:#fffbeb; color:#92400e; font-size:13px; line-height:1.45;\">");
            html.append("<strong>Trabajadores sin correo en base de datos:</strong> ");
            html.append(escapeHtml(String.join(", ", workersWithoutEmail.stream().limit(12).toList())));
            if (workersWithoutEmail.size() > 12) {
                html.append(escapeHtml(" y " + (workersWithoutEmail.size() - 12) + " mas"));
            }
            html.append(". Actualiza la ficha del trabajador antes de aprobar o rechazar para que reciba la notificacion.</div>");
        }
        html.append("<p style=\"margin:0 0 14px;\"><a href=\"")
                .append(escapeHtml(reviewUrl))
                .append("\" style=\"display:inline-block; padding:10px 14px; background:#16a34a; color:#ffffff; text-decoration:none; border-radius:7px; font-weight:bold; font-size:13px;\">Abrir panel de revision</a></p>");
        html.append("<p style=\"margin:0; font-size:12px; line-height:1.5; color:#6b7280;\">Este correo solo informa que hay documentos por revisar. El detalle completo esta disponible en la aplicacion.</p>");
        html.append("<p style=\"margin:14px 0 0; font-size:12px; line-height:1.5; color:#111827;\">Sistema de gestion de conceptos de aptitud para trabajo en alturas</p>");
        html.append("</div></div></div></body></html>");

        return html.toString();
    }

    private void sendHtmlEmail(String to, String cc, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        if (!safe(from).isBlank()) {
            helper.setFrom(from.trim());
        }

        helper.setTo(splitRecipients(to));

        String[] ccRecipients = splitRecipients(cc);

        if (ccRecipients.length > 0) {
            helper.setCc(ccRecipients);
        }

        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
    }

    private void updateDocumentNotificationStatus(
            ManagedDocument document,
            String status,
            String errorMessage) {
        document.setNotificationStatus(status);

        if (STATUS_SENT.equals(status)) {
            document.setNotifiedAt(LocalDateTime.now());
        }

        document.setNotificationError(errorMessage);
        managedDocumentRepository.save(document);
    }

    private EmailLog finishApproverLog(
            DocumentBatch batch,
            EmailLog log,
            String status,
            String reason) {
        log.setStatus(status);
        log.setErrorMessage(reason);

        EmailLog savedLog = emailLogRepository.save(log);

        batch.setApproverNotificationStatus(status);
        batch.setApproverNotificationError(reason);
        documentBatchRepository.save(batch);

        return savedLog;
    }

    private Employee resolveEmployee(String employeeId) {
        if (safe(employeeId).isBlank()) {
            return null;
        }

        return employeeRepository.findById(employeeId).orElse(null);
    }

    private DocumentAnalysis resolveAnalysis(String documentId) {
        if (safe(documentId).isBlank()) {
            return null;
        }

        return documentAnalysisRepository.findByDocumentId(documentId).orElse(null);
    }

    private EmailLog saveSkippedLog(
            String documentId,
            String type,
            String reason) {
        EmailLog log = new EmailLog();
        log.setDocumentId(documentId);
        log.setType(type);
        log.setTriggeredBy("AUTO");
        log.setAttemptedAt(LocalDateTime.now());
        log.setStatus(STATUS_SKIPPED);
        log.setErrorMessage(reason);
        log.setCreatedAt(LocalDateTime.now());

        return emailLogRepository.save(log);
    }

    private EmailLog saveSkippedLog(
            ManagedDocument document,
            Employee employee,
            DocumentAnalysis analysis,
            String type,
            String reason) {
        EmailLog log = buildBaseLog(document, employee, analysis);
        log.setType(type);
        log.setTriggeredBy(WORKER_NOTIFICATION_RESEND.equals(type) ? "MANUAL" : "AUTO");
        log.setAttemptedAt(LocalDateTime.now());
        log.setStatus(STATUS_SKIPPED);
        log.setErrorMessage(reason);

        return emailLogRepository.save(log);
    }

    private EmailLog buildBaseLog(
            ManagedDocument document,
            Employee employee,
            DocumentAnalysis analysis) {
        EmailLog log = new EmailLog();

        log.setDocumentId(document.getId());
        log.setBatchId(document.getBatchId());
        log.setEmployeeId(employee.getId());
        log.setEmployeeDocumentNumber(employee.getDocumentNumber());
        log.setResultStatus(normalizeResultStatus(analysis.getResultStatus()));
        log.setCreatedAt(LocalDateTime.now());

        return log;
    }

    private String normalizeResultStatus(String resultStatus) {
        String value = safe(resultStatus).toUpperCase();

        if ("APTO".equals(value)) {
            return "APTO";
        }

        if ("NO_APTO".equals(value) || "NO APTO".equals(value)) {
            return "NO_APTO";
        }

        return "PENDIENTE";
    }

    private String buildEmployeeName(Employee employee) {
        if (employee == null) {
            return "";
        }

        return String.join(
                " ",
                safe(employee.getFirstName()),
                safe(employee.getSecondName()),
                safe(employee.getFirstLastName()),
                safe(employee.getSecondLastName()))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String[] splitRecipients(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }

        return Arrays.stream(value.split("[,;\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    private String buildReadableMailError(Exception ex) {
        String message = safe(ex.getMessage());

        Throwable cause = ex.getCause();

        if (cause != null && !safe(cause.getMessage()).isBlank()) {
            message = message + " | Causa: " + cause.getMessage();
        }

        return message.isBlank()
                ? "No se pudo enviar el correo por un error SMTP."
                : message;
    }

    private boolean wasPartiallySent(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SendFailedException sendFailedException) {
                return sendFailedException.getValidSentAddresses() != null &&
                        sendFailedException.getValidSentAddresses().length > 0;
            }

            if (current instanceof MailSendException mailSendException) {
                for (Exception exception : mailSendException.getMessageExceptions()) {
                    if (wasPartiallySent(exception)) {
                        return true;
                    }
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private String valueOrDash(String value) {
        String text = safe(value);
        return text.isBlank() ? "-" : text;
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }
}
