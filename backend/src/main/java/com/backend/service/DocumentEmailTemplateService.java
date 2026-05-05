package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
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

    @Value("${app.email.default-cc:}")
    private String defaultCc;

    @Value("${app.email.signature-name:}")
    private String signatureName;

    @Value("${app.email.signature-role:}")
    private String signatureRole;

    @Value("${app.email.signature-phone:}")
    private String signaturePhone;

    @Value("${app.email.signature-address:}")
    private String signatureAddress;

    public DocumentEmailTemplateService(
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository
    ) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
    }

    public Map<String, String> buildTemplate(String documentId) {
        ManagedDocument document = managedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento."));

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el análisis del documento."));

        Employee employee = null;
        if (document.getEmployeeId() != null && !document.getEmployeeId().isBlank()) {
            employee = employeeRepository.findById(document.getEmployeeId()).orElse(null);
        }

        String resultStatus = normalizeResultStatus(analysis.getResultStatus());
        String to = resolveRecipient(employee);
        String cc = safe(defaultCc);
        String subject = buildSubject(resultStatus);
        String body = buildBody(resultStatus);

        Map<String, String> template = new LinkedHashMap<>();
        template.put("to", to);
        template.put("cc", cc);
        template.put("subject", subject);
        template.put("body", body);
        return template;
    }

    private String normalizeResultStatus(String resultStatus) {
        if ("APTO".equalsIgnoreCase(safe(resultStatus))) {
            return "APTO";
        }
        if ("NO_APTO".equalsIgnoreCase(safe(resultStatus))) {
            return "NO_APTO";
        }
        return "PENDIENTE";
    }

    private String resolveRecipient(Employee employee) {
        if (employee != null && employee.getEmail() != null && !employee.getEmail().isBlank()) {
            return employee.getEmail().trim();
        }
        return "";
    }

    private String buildSubject(String resultStatus) {
        return switch (resultStatus) {
            case "NO_APTO" -> "RESTRICCIÓN PARA TRABAJO EN ALTURAS";
            case "APTO" -> "LEVANTAMIENTO DE RESTRICCIÓN TRABAJO EN ALTURAS";
            default -> "SEGUIMIENTO DE CONCEPTO MÉDICO PARA TRABAJO EN ALTURAS";
        };
    }

    private String buildBody(String resultStatus) {
        return switch (resultStatus) {
            case "NO_APTO" -> joinLines(
                    "Buenas tardes,",
                    "",
                    "Me permito manifestarle que según el concepto médico para trabajo en alturas usted presenta una restricción temporal, por lo cual debe continuar en control y tratamiento para la condición identificada.",
                    "",
                    buildSignature()
            );
            case "APTO" -> joinLines(
                    "Buenas tardes,",
                    "",
                    "Le comunico que con base en el concepto médico reciente para trabajo en alturas usted se encuentra sin restricción, por lo que puede continuar realizando sus actividades normalmente.",
                    "",
                    "Es importante mantener sus controles médicos y fortalecer estilos de vida saludable.",
                    "",
                    buildSignature()
            );
            default -> joinLines(
                    "Buenas tardes,",
                    "",
                    "Le informo que el concepto médico para trabajo en alturas se encuentra pendiente de validación o revisión final.",
                    "",
                    "Una vez se tenga la confirmación correspondiente, se realizará la comunicación formal.",
                    "",
                    buildSignature()
            );
        };
    }

    private String buildSignature() {
        return joinLines(
                "Cordialmente,",
                safe(signatureName),
                safe(signatureRole),
                safe(signaturePhone),
                safe(signatureAddress)
        );
    }

    private String joinLines(String... lines) {
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            String value = line == null ? "" : line;
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(value);
        }

        return sb.toString().trim();
    }

    private String safe(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }
}