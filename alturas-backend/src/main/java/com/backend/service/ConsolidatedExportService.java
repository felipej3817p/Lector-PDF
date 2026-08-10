package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.DocumentAnalysis;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsolidatedExportService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConsolidatedExportService(
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
    }

    /**
     * Exporta el consolidado CSV completo dentro del alcance del usuario
     * autenticado.
     */
    public byte[] exportConsolidatedCsv() {
        List<ManagedDocument> scopedDocuments = getScopedDocuments();

        List<ConsolidatedRow> rows = scopedDocuments.stream()
                .map(this::toConsolidatedRow)
                .sorted(Comparator.comparing(ConsolidatedRow::uploadedAtSafe,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return buildCsv(rows).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Alias por compatibilidad si el controlador viejo usa este nombre.
     */
    public byte[] generateConsolidatedCsv() {
        return exportConsolidatedCsv();
    }

    /**
     * Alias adicional por compatibilidad.
     */
    public byte[] exportCsv() {
        return exportConsolidatedCsv();
    }

    private List<ManagedDocument> getScopedDocuments() {
        User currentUser = accessScopeService.getCurrentUser();

        if (accessScopeService.hasGlobalDocumentAccess(currentUser)) {
            return managedDocumentRepository.findAll();
        }

        Set<AreaCode> allowedAreas = accessScopeService.getAllowedAreas(currentUser);
        if (allowedAreas == null || allowedAreas.isEmpty()) {
            return Collections.emptyList();
        }

        return managedDocumentRepository.findByAreaCodeIn(allowedAreas);
    }

    private ConsolidatedRow toConsolidatedRow(ManagedDocument document) {
        Optional<DocumentAnalysis> analysisOpt = documentAnalysisRepository.findByDocumentId(document.getId());

        DocumentAnalysis analysis = analysisOpt.orElse(null);

        Employee employee = null;
        if (document.getEmployeeId() != null && !document.getEmployeeId().isBlank()) {
            employee = employeeRepository.findById(document.getEmployeeId()).orElse(null);
        }

        String resultStatus = normalizeResultStatus(analysis != null ? analysis.getResultStatus() : null);

        String patientName = firstNonBlank(
                buildEmployeeFullName(employee),
                extractStringField(analysis, "patientName"));

        String documentNumber = firstNonBlank(
                employee != null ? safe(employee.getDocumentNumber()) : "",
                extractStringField(analysis, "documentNumber"));

        String position = firstNonBlank(
                employee != null ? safe(employee.getCurrentPosition()) : "",
                extractStringField(analysis, "position"));

        String workArea = firstNonBlank(
                employee != null ? safe(employee.getWorkArea()) : "",
                extractStringField(analysis, "workArea"));

        String zone = firstNonBlank(
                employee != null ? safe(employee.getZone()) : "",
                extractStringField(analysis, "zone"));

        String email = employee != null ? safe(employee.getEmail()) : "";
        String employer = employee != null ? safe(employee.getEmployer()) : "";
        String arl = firstNonBlank(
                employee != null ? safe(employee.getArl()) : "",
                extractStringField(analysis, "arl"));

        String laborConcept = extractStringField(analysis, "laborConcept");
        String observations = extractStringField(analysis, "observations");
        String surveillanceProgram = extractStringField(analysis, "surveillanceProgram");
        String referrals = extractListField(analysis, "referrals");
        String examType = firstNonBlank(
                safe(document.getExamType()),
                extractStringField(analysis, "examType"));

        String analysisState = analysis != null
                ? "ANALIZADO"
                : normalizeProcessingStatus(document.getProcessingStatus());

        return new ConsolidatedRow(
                document.getUploadedAt(),
                safe(document.getOriginalFileName()),
                safe(document.getDocumentType()),
                safe(examType),
                safe(patientName),
                safe(documentNumber),
                safe(position),
                safe(workArea),
                safe(zone),
                safe(email),
                safe(employer),
                safe(arl),
                document.getAreaCode() != null ? document.getAreaCode().name() : "",
                safe(resultStatus),
                safe(analysisState),
                safe(laborConcept),
                safe(observations),
                safe(surveillanceProgram),
                safe(referrals),
                safe(document.getUploadedBy()));
    }

    private String buildCsv(List<ConsolidatedRow> rows) {
        StringBuilder sb = new StringBuilder();

        // BOM UTF-8 para Excel/Windows
        sb.append('\uFEFF');

        sb.append(String.join(",",
                "fecha_carga",
                "hora_carga",
                "fecha_hora_carga",
                "archivo_pdf",
                "tipo_documento_archivo",
                "tipo_examen",
                "funcionario",
                "identificacion",
                "cargo",
                "zona",
                "correo",
                "empleador",
                "arl",
                "area_codigo",
                "resultado",
                "estado_analisis",
                "concepto_laboral",
                "observaciones",
                "programa_vigilancia",
                "remisiones",
                "subido_por")).append("\n");

        for (ConsolidatedRow row : rows) {
            sb.append(csv(row.uploadedAt != null ? row.uploadedAt.format(DATE_FORMAT) : ""))
                    .append(",")
                    .append(csv(row.uploadedAt != null ? row.uploadedAt.format(TIME_FORMAT) : ""))
                    .append(",")
                    .append(csv(row.uploadedAt != null ? row.uploadedAt.format(DATE_TIME_FORMAT) : ""))
                    .append(",")
                    .append(csv(row.originalFileName))
                    .append(",")
                    .append(csv(row.documentType))
                    .append(",")
                    .append(csv(row.examType))
                    .append(",")
                    .append(csv(row.fullName))
                    .append(",")
                    .append(csv(row.documentNumber))
                    .append(",")
                    .append(csv(row.position))
                    .append(",")
                    .append(csv(row.zone))
                    .append(",")
                    .append(csv(row.email))
                    .append(",")
                    .append(csv(row.employer))
                    .append(",")
                    .append(csv(row.arl))
                    .append(",")
                    .append(csv(row.areaCode))
                    .append(",")
                    .append(csv(row.resultStatus))
                    .append(",")
                    .append(csv(row.analysisState))
                    .append(",")
                    .append(csv(row.laborConcept))
                    .append(",")
                    .append(csv(row.observations))
                    .append(",")
                    .append(csv(row.surveillanceProgram))
                    .append(",")
                    .append(csv(row.referrals))
                    .append(",")
                    .append(csv(row.uploadedBy))
                    .append("\n");
        }

        return sb.toString();
    }

    private String csv(String value) {
        String text = value == null ? "" : value;
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String normalizeResultStatus(String resultStatus) {
        String normalized = safe(resultStatus).toUpperCase(Locale.ROOT);
        if ("APTO".equals(normalized)) {
            return "APTO";
        }
        if ("NO_APTO".equals(normalized) || "NO APTO".equals(normalized)) {
            return "NO_APTO";
        }
        return "NO_APTO";
    }

    private String normalizeProcessingStatus(String processingStatus) {
        String normalized = safe(processingStatus).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "PENDIENTE";
        }
        return normalized;
    }

    private String buildEmployeeFullName(Employee employee) {
        if (employee == null) {
            return "";
        }

        return Arrays.asList(
                safe(employee.getFirstName()),
                safe(employee.getSecondName()),
                safe(employee.getFirstLastName()),
                safe(employee.getSecondLastName())).stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }

    private String extractStringField(DocumentAnalysis analysis, String fieldName) {
        if (analysis == null || analysis.getExtractedFields() == null) {
            return "";
        }

        Object extractedFields = analysis.getExtractedFields();

        if (extractedFields instanceof Map<?, ?> map) {
            Object value = map.get(fieldName);
            return value != null ? safe(value.toString()) : "";
        }

        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Object value = extractedFields.getClass().getMethod(getterName).invoke(extractedFields);
            return value != null ? safe(value.toString()) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractListField(DocumentAnalysis analysis, String fieldName) {
        if (analysis == null || analysis.getExtractedFields() == null) {
            return "";
        }

        Object extractedFields = analysis.getExtractedFields();

        if (extractedFields instanceof Map<?, ?> map) {
            Object value = map.get(fieldName);
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(this::safe)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining("; "));
            }
            return value != null ? safe(value.toString()) : "";
        }

        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Object value = extractedFields.getClass().getMethod(getterName).invoke(extractedFields);

            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(this::safe)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining("; "));
            }

            return value != null ? safe(value.toString()) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ConsolidatedRow {
        private final LocalDateTime uploadedAt;
        private final String originalFileName;
        private final String documentType;
        private final String examType;
        private final String fullName;
        private final String documentNumber;
        private final String position;
        private final String workArea;
        private final String zone;
        private final String email;
        private final String employer;
        private final String arl;
        private final String areaCode;
        private final String resultStatus;
        private final String analysisState;
        private final String laborConcept;
        private final String observations;
        private final String surveillanceProgram;
        private final String referrals;
        private final String uploadedBy;

        private ConsolidatedRow(
                LocalDateTime uploadedAt,
                String originalFileName,
                String documentType,
                String examType,
                String fullName,
                String documentNumber,
                String position,
                String workArea,
                String zone,
                String email,
                String employer,
                String arl,
                String areaCode,
                String resultStatus,
                String analysisState,
                String laborConcept,
                String observations,
                String surveillanceProgram,
                String referrals,
                String uploadedBy) {
            this.uploadedAt = uploadedAt;
            this.originalFileName = originalFileName;
            this.documentType = documentType;
            this.examType = examType;
            this.fullName = fullName;
            this.documentNumber = documentNumber;
            this.position = position;
            this.workArea = workArea;
            this.zone = zone;
            this.email = email;
            this.employer = employer;
            this.arl = arl;
            this.areaCode = areaCode;
            this.resultStatus = resultStatus;
            this.analysisState = analysisState;
            this.laborConcept = laborConcept;
            this.observations = observations;
            this.surveillanceProgram = surveillanceProgram;
            this.referrals = referrals;
            this.uploadedBy = uploadedBy;
        }

        private LocalDateTime uploadedAtSafe() {
            return uploadedAt;
        }
    }
}
