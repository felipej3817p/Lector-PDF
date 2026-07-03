package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmailLogRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeHistoryService {

    private final EmployeeRepository employeeRepository;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmailLogRepository emailLogRepository;
    private final AccessScopeService accessScopeService;

    public EmployeeHistoryService(
            EmployeeRepository employeeRepository,
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmailLogRepository emailLogRepository,
            AccessScopeService accessScopeService
    ) {
        this.employeeRepository = employeeRepository;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.emailLogRepository = emailLogRepository;
        this.accessScopeService = accessScopeService;
    }

    public List<Map<String, Object>> getEmployeeHistory(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el trabajador."));

        assertCanViewEmployee(employee);

        List<ManagedDocument> documents = managedDocumentRepository.findByEmployeeIdOrderByUploadedAtDesc(employeeId);

        return documents.stream()
                .map(document -> buildHistoryItem(employee, document))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildHistoryItem(Employee employee, ManagedDocument document) {
        Optional<DocumentAnalysis> analysisOptional =
                documentAnalysisRepository.findByDocumentId(document.getId());

        List<EmailLog> emailLogs =
                emailLogRepository.findByDocumentIdOrderByCreatedAtDesc(document.getId());

        Optional<EmailLog> lastWorkerNotification = emailLogs.stream()
                .filter(log -> "WORKER_NOTIFICATION".equalsIgnoreCase(safe(log.getType())))
                .findFirst();

        Optional<EmailLog> lastApproverNotification = emailLogs.stream()
                .filter(log -> "APPROVER_BATCH_NOTIFICATION".equalsIgnoreCase(safe(log.getType())))
                .findFirst();

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("documentId", document.getId());
        item.put("employeeId", employee.getId());
        item.put("employeeDocumentNumber", employee.getDocumentNumber());
        item.put("employeeName", buildEmployeeName(employee));

        item.put("originalFileName", document.getOriginalFileName());
        item.put("documentType", document.getDocumentType());
        item.put("examType", document.getExamType());
 
        item.put("batchId", document.getBatchId());
        item.put("batchCode", document.getBatchCode());

        item.put("uploadedAt", document.getUploadedAt());
        item.put("processingStatus", document.getProcessingStatus());
        item.put("reviewStatus", resolveEffectiveReviewStatus(document, lastWorkerNotification));
        item.put("notificationStatus", resolveEffectiveNotificationStatus(document, lastWorkerNotification));

        item.put("reviewedBy", document.getReviewedBy());
        item.put("reviewedAt", document.getReviewedAt());
        item.put("reviewComment", document.getReviewComment());

        if (analysisOptional.isPresent()) {
            DocumentAnalysis analysis = analysisOptional.get();

            item.put("analysisId", analysis.getId());
            item.put("resultStatus", normalizeResultStatus(analysis.getResultStatus()));
            item.put("analyzedAt", analysis.getAnalyzedAt());
            item.put("conceptDate", resolveEvaluationDate(analysis, document));
            item.put("fechaConcepto", resolveEvaluationDate(analysis, document));
            item.put("fechaEvaluacion", resolveEvaluationDate(analysis, document));
            item.put("evaluationDate", resolveEvaluationDate(analysis, document));
        } else {
            item.put("analysisId", "");
            item.put("resultStatus", "PENDIENTE");
            item.put("analyzedAt", null);
            item.put("conceptDate", null);
            item.put("fechaConcepto", null);
            item.put("fechaEvaluacion", null);
            item.put("evaluationDate", null);
        }

        item.put("workerEmailStatus", lastWorkerNotification.map(EmailLog::getStatus).orElse(""));
        item.put("workerEmailTo", lastWorkerNotification.map(EmailLog::getTo).orElse(""));
        item.put("workerEmailCc", lastWorkerNotification.map(EmailLog::getCc).orElse(""));
        item.put("workerEmailSentAt", lastWorkerNotification.map(EmailLog::getSentAt).orElse(null));
        item.put("workerEmailError", lastWorkerNotification.map(EmailLog::getErrorMessage).orElse(""));

        item.put("approverEmailStatus", lastApproverNotification.map(EmailLog::getStatus).orElse(""));
        item.put("approverEmailSentAt", lastApproverNotification.map(EmailLog::getSentAt).orElse(null));

        item.put("emailLogsCount", emailLogs.size());

        return item;
    }

    private String resolveEffectiveReviewStatus(
            ManagedDocument document,
            Optional<EmailLog> lastWorkerNotification
    ) {
        String reviewStatus = safe(document.getReviewStatus()).toUpperCase(Locale.ROOT);

        if ("APPROVED".equals(reviewStatus) || "REJECTED".equals(reviewStatus)) {
            return reviewStatus;
        }

        if (lastWorkerNotification.isEmpty()) {
            return safe(document.getReviewStatus());
        }

        return isRejectedNotification(lastWorkerNotification.get()) ? "REJECTED" : "APPROVED";
    }

    private String resolveEffectiveNotificationStatus(
            ManagedDocument document,
            Optional<EmailLog> lastWorkerNotification
    ) {
        if (lastWorkerNotification.isEmpty()) {
            return safe(document.getNotificationStatus());
        }

        String workerStatus = safe(lastWorkerNotification.get().getStatus()).toUpperCase(Locale.ROOT);
        String documentStatus = safe(document.getNotificationStatus()).toUpperCase(Locale.ROOT);

        if ("SENT".equals(workerStatus) || "SENT".equals(documentStatus)) {
            return "SENT";
        }

        if ("FAILED".equals(workerStatus)) {
            return "FAILED";
        }

        if ("SKIPPED".equals(workerStatus)) {
            return "SKIPPED";
        }

        return safe(document.getNotificationStatus());
    }

    private boolean isRejectedNotification(EmailLog log) {
        String text = (safe(log.getSubject()) + " " + safe(log.getBody())).toLowerCase(Locale.ROOT);
        return text.contains("rechaz");
    }

    private void assertCanViewEmployee(Employee employee) {
        User currentUser = accessScopeService.getCurrentUser();

        if (accessScopeService.hasGlobalDocumentAccess(currentUser)) {
            return;
        }

        if (employee.getAreaCode() == null) {
            throw new IllegalArgumentException("No tiene permisos para consultar este trabajador.");
        }

        if (!accessScopeService.getAllowedAreas(currentUser).contains(employee.getAreaCode())) {
            throw new IllegalArgumentException("No tiene permisos para consultar este trabajador.");
        }
    }

    private String resolveEvaluationDate(DocumentAnalysis analysis, ManagedDocument document) {
        if (document != null && document.getFechaConcepto() != null) {
            return document.getFechaConcepto().toString();
        }

        if (analysis != null && analysis.getEvaluationDate() != null) {
            return analysis.getEvaluationDate().toString();
        }

        LocalDate extractedDate = extractDateFromFields(analysis != null ? analysis.getExtractedFields() : null);

        if (extractedDate != null) {
            return extractedDate.toString();
        }

        return "";
    }

    private LocalDate extractDateFromFields(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        String[] keys = {
                "evaluationDate",
                "fechaEvaluacion",
                "conceptDate",
                "fechaConcepto",
                "examDate",
                "fechaExamen",
                "date",
                "fecha"
        };

        for (String key : keys) {
            Object value = fields.get(key);

            if (value == null) {
                continue;
            }

            try {
                return LocalDate.parse(String.valueOf(value));
            } catch (Exception ignored) {
                // Mantiene historial estable si el PDF trae una fecha no ISO.
            }
        }

        return null;
    }

    private String normalizeResultStatus(String value) {
        String normalized = safe(value)
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .trim();

        if ("NO_APTO".equals(normalized) || "NO_APTO_TEMPORAL".equals(normalized)) {
            return "NO_APTO";
        }

        if ("APTO".equals(normalized)) {
            return "APTO";
        }

        return "PENDIENTE";
    }

    private String buildEmployeeName(Employee employee) {
        return String.join(
                        " ",
                        safe(employee.getFirstName()),
                        safe(employee.getSecondName()),
                        safe(employee.getFirstLastName()),
                        safe(employee.getSecondLastName())
                )
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
