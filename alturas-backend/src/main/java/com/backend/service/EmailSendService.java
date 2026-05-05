package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmailLogRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailSendService {

    private final JavaMailSender mailSender;
    private final DocumentEmailTemplateService documentEmailTemplateService;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailLogRepository emailLogRepository;

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
            EmailLogRepository emailLogRepository
    ) {
        this.mailSender = mailSender;
        this.documentEmailTemplateService = documentEmailTemplateService;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
        this.emailLogRepository = emailLogRepository;
    }

    public EmailLog sendAnalysisEmailIfEnabled(String documentId) {
        if (!autoSendEnabled) {
            return saveSkippedLog(documentId, "El envío automático de correos está deshabilitado.");
        }

        return sendAnalysisEmail(documentId);
    }

    public EmailLog sendAnalysisEmail(String documentId) {
        ManagedDocument document = managedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento."));

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el análisis del documento."));

        Employee employee = employeeRepository.findById(document.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el trabajador asociado."));

        String resultStatus = normalizeResultStatus(analysis.getResultStatus());

        if (!"APTO".equals(resultStatus) && !"NO_APTO".equals(resultStatus)) {
            return saveSkippedLog(document, employee, analysis, "Resultado pendiente o no concluyente. No se envía correo.");
        }

        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            return saveSkippedLog(document, employee, analysis, "El trabajador no tiene correo registrado.");
        }

        if (emailLogRepository.existsByDocumentIdAndResultStatusAndStatus(documentId, resultStatus, "SENT")) {
            return saveSkippedLog(document, employee, analysis, "Ya existe un correo enviado para este documento y resultado.");
        }

        Map<String, String> template = documentEmailTemplateService.buildTemplate(documentId);

        String to = safe(template.get("to"));
        String cc = safe(template.get("cc"));
        String subject = safe(template.get("subject"));
        String body = safe(template.get("body"));

        if (to.isBlank()) {
            return saveSkippedLog(document, employee, analysis, "No se pudo resolver destinatario.");
        }

        EmailLog log = buildBaseLog(document, employee, analysis);
        log.setTo(to);
        log.setCc(cc);
        log.setSubject(subject);
        log.setBody(body);

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            if (!safe(from).isBlank()) {
                message.setFrom(from.trim());
            }

            message.setTo(to);

            String[] ccRecipients = splitRecipients(cc);
            if (ccRecipients.length > 0) {
                message.setCc(ccRecipients);
            }

            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.setStatus("SENT");
            log.setSentAt(LocalDateTime.now());
            log.setErrorMessage("");

            return emailLogRepository.save(log);
        } catch (MailException ex) {
            log.setStatus("FAILED");
            log.setErrorMessage(ex.getMessage());
            return emailLogRepository.save(log);
        }
    }

    private EmailLog saveSkippedLog(String documentId, String reason) {
        EmailLog log = new EmailLog();
        log.setDocumentId(documentId);
        log.setStatus("SKIPPED");
        log.setErrorMessage(reason);
        log.setCreatedAt(LocalDateTime.now());
        return emailLogRepository.save(log);
    }

    private EmailLog saveSkippedLog(
            ManagedDocument document,
            Employee employee,
            DocumentAnalysis analysis,
            String reason
    ) {
        EmailLog log = buildBaseLog(document, employee, analysis);
        log.setStatus("SKIPPED");
        log.setErrorMessage(reason);
        return emailLogRepository.save(log);
    }

    private EmailLog buildBaseLog(
            ManagedDocument document,
            Employee employee,
            DocumentAnalysis analysis
    ) {
        EmailLog log = new EmailLog();
        log.setDocumentId(document.getId());
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

    private String[] splitRecipients(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    private String safe(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }
}