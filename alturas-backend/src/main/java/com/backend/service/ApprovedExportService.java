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
public class ApprovedExportService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ApprovedExportService(
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService
    ) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
    }

    /**
     * Exporta solo registros APTO dentro del alcance del usuario autenticado.
     */
    public byte[] exportApprovedCsv() {
        List<ManagedDocument> scopedDocuments = getScopedDocuments();

        List<ApprovedRow> rows = scopedDocuments.stream()
                .map(this::toApprovedRowOrNull)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ApprovedRow::uploadedAtSafe, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return buildCsv(rows).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Alias útil si tu controlador viejo llamaba generateApprovedCsv().
     */
    public byte[] generateApprovedCsv() {
        return exportApprovedCsv();
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

    private ApprovedRow toApprovedRowOrNull(ManagedDocument document) {
        Optional<DocumentAnalysis> analysisOpt = documentAnalysisRepository.findByDocumentId(document.getId());
        if (analysisOpt.isEmpty()) {
            return null;
        }

        DocumentAnalysis analysis = analysisOpt.get();
        String resultStatus = safe(analysis.getResultStatus()).toUpperCase(Locale.ROOT);

        if (!"APTO".equals(resultStatus)) {
            return null;
        }

        Employee employee = null;
        if (document.getEmployeeId() != null && !document.getEmployeeId().isBlank()) {
            employee = employeeRepository.findById(document.getEmployeeId()).orElse(null);
        }

        String patientName = firstNonBlank(
                employeeFullName(employee),
                extractStringField(analysis, "patientName")
        );

        String documentNumber = firstNonBlank(
                employee != null ? safe(employee.getDocumentNumber()) : "",
                extractStringField(analysis, "documentNumber")
        );

        String position = firstNonBlank(
                employee != null ? safe(employee.getCurrentPosition()) : "",
                extractStringField(analysis, "position")
        );

        String workArea = employee != null ? safe(employee.getWorkArea()) : "";
        String zone = employee != null ? safe(employee.getZone()) : "";
        String areaCode = document.getAreaCode() != null ? document.getAreaCode().name() : "";
        String laborConcept = extractStringField(analysis, "laborConcept");
        String observations = extractStringField(analysis, "observations");
        String referrals = extractListField(analysis, "referrals");

        return new ApprovedRow(
                document.getUploadedAt(),
                safe(document.getOriginalFileName()),
                safe(patientName),
                safe(documentNumber),
                safe(position),
                safe(workArea),
                safe(zone),
                safe(areaCode),
                resultStatus,
                safe(laborConcept),
                safe(observations),
                safe(referrals),
                safe(document.getUploadedBy())
        );
    }

    private String buildCsv(List<ApprovedRow> rows) {
        StringBuilder sb = new StringBuilder();

        // BOM para Excel/Windows
        sb.append('\uFEFF');

        sb.append(String.join(",",
                "fecha_carga",
                "hora_carga",
                "archivo_pdf",
                "funcionario",
                "identificacion",
                "cargo",
                "zona",
                "area_codigo",
                "resultado",
                "concepto_laboral",
                "observaciones",
                "remisiones",
                "subido_por"
        )).append("\n");

        for (ApprovedRow row : rows) {
            sb.append(csv(row.uploadedAt != null ? row.uploadedAt.format(DATE_FORMAT) : ""))
              .append(",")
              .append(csv(row.uploadedAt != null ? row.uploadedAt.format(TIME_FORMAT) : ""))
              .append(",")
              .append(csv(row.originalFileName))
              .append(",")
              .append(csv(row.fullName))
              .append(",")
              .append(csv(row.documentNumber))
              .append(",")
              .append(csv(row.position))
              .append(",")
              .append(csv(row.zone))
              .append(",")
              .append(csv(row.areaCode))
              .append(",")
              .append(csv(row.resultStatus))
              .append(",")
              .append(csv(row.laborConcept))
              .append(",")
              .append(csv(row.observations))
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

    private String employeeFullName(Employee employee) {
        if (employee == null) {
            return "";
        }

        return StreamBuilder.of(
                        safe(employee.getFirstName()),
                        safe(employee.getSecondName()),
                        safe(employee.getFirstLastName()),
                        safe(employee.getSecondLastName())
                )
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

    private static final class ApprovedRow {
        private final LocalDateTime uploadedAt;
        private final String originalFileName;
        private final String fullName;
        private final String documentNumber;
        private final String position;
        private final String workArea;
        private final String zone;
        private final String areaCode;
        private final String resultStatus;
        private final String laborConcept;
        private final String observations;
        private final String referrals;
        private final String uploadedBy;

        private ApprovedRow(
                LocalDateTime uploadedAt,
                String originalFileName,
                String fullName,
                String documentNumber,
                String position,
                String workArea,
                String zone,
                String areaCode,
                String resultStatus,
                String laborConcept,
                String observations,
                String referrals,
                String uploadedBy
        ) {
            this.uploadedAt = uploadedAt;
            this.originalFileName = originalFileName;
            this.fullName = fullName;
            this.documentNumber = documentNumber;
            this.position = position;
            this.workArea = workArea;
            this.zone = zone;
            this.areaCode = areaCode;
            this.resultStatus = resultStatus;
            this.laborConcept = laborConcept;
            this.observations = observations;
            this.referrals = referrals;
            this.uploadedBy = uploadedBy;
        }

        private LocalDateTime uploadedAtSafe() {
            return uploadedAt;
        }
    }

    private static final class StreamBuilder {
        private StreamBuilder() {
        }

        @SafeVarargs
        private static <T> java.util.stream.Stream<T> of(T... values) {
            return Arrays.stream(values);
        }
    }
}
