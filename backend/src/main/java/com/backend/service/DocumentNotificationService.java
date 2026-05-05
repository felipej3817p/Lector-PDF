package com.backend.service;

import com.backend.model.ManagedDocument;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DocumentNotificationService {
    private final JavaMailSender mailSender;
    private final DocumentEmailTemplateService templateService;

    public DocumentNotificationService(JavaMailSender mailSender, DocumentEmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    public void notifyEmployee(ManagedDocument document) {
        Map<String, String> template = templateService.buildTemplate(document.getId());
        String to = safe(template.get("to"));
        if (to.isBlank()) {
            document.setNotificationStatus("SKIPPED");
            document.setNotificationError("El empleado no tiene correo registrado.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        String cc = safe(template.get("cc"));
        if (!cc.isBlank()) {
            message.setCc(cc.split("\\s*,\\s*"));
        }
        message.setSubject(safe(template.get("subject")));
        message.setText(safe(template.get("body")));

        try {
            mailSender.send(message);
            document.setNotificationStatus("SENT");
            document.setNotificationSentAt(LocalDateTime.now());
            document.setNotificationError(null);
        } catch (Exception ex) {
            document.setNotificationStatus("FAILED");
            document.setNotificationError(ex.getMessage());
        }
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
}
