package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DocumentAnalysisService {

    private static final String UPLOAD_DIR = "uploads/documents";

    private final DocumentService documentService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final PdfFieldParserService pdfFieldParserService;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmployeeRepository employeeRepository;

    public DocumentAnalysisService(
            DocumentService documentService,
            PdfTextExtractorService pdfTextExtractorService,
            PdfFieldParserService pdfFieldParserService,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmployeeRepository employeeRepository
    ) {
        this.documentService = documentService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.pdfFieldParserService = pdfFieldParserService;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.employeeRepository = employeeRepository;
    }

    public Map<String, Object> analyze(String documentId) {
        ManagedDocument document = documentService.findById(documentId);

        Path filePath = resolveFilePath(document);

        String extractedText = pdfTextExtractorService.extractText(filePath);

        String normalizedText = extractedText != null
                ? extractedText.toUpperCase().replaceAll("\\s+", " ").trim()
                : "";

        String resultStatus = resolveResultStatus(normalizedText);

        Map<String, Object> extractedFields = pdfFieldParserService.extractFields(extractedText);
        LocalDate fechaConcepto = resolveFechaConcepto(extractedFields);

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseGet(DocumentAnalysis::new);

        analysis.setDocumentId(document.getId());
        analysis.setEmployeeId(document.getEmployeeId());
        analysis.setResultStatus(resultStatus);
        analysis.setExtractedFields(extractedFields);
        analysis.setExtractedText(extractedText);
        analysis.setEvaluationDate(fechaConcepto);
        analysis.setAnalyzedAt(Instant.now());

        analysis = documentAnalysisRepository.save(analysis);

        /*
         * Flujo correcto:
         *
         * 1. El analisis del PDF NO envia correo al trabajador.
         * 2. El analisis solo deja el documento pendiente de revision.
         * 3. El correo al trabajador se envia unicamente cuando el aprobador aprueba.
         * 4. Si el aprobador rechaza, no se envia correo al trabajador.
         */
        document.setFechaConcepto(fechaConcepto);

        if (document.isHistorical()) {
            document.setProcessingStatus("STORED");
            document.setReviewStatus("NOT_PENDING");
            document.setNotificationStatus("NOT_PENDING");
            document.setNotificationError("Carga historica: no se enviaron correos.");
        } else if ("APTO".equals(resultStatus) || "NO_APTO".equals(resultStatus)) {
            document.setProcessingStatus("ANALYZED");
            document.setReviewStatus("PENDING_REVIEW");
            document.setNotificationStatus("NOT_PENDING");
        } else {
            document.setProcessingStatus("PENDING_MANUAL_REVIEW");
            document.setReviewStatus("PENDING_REVIEW");
            document.setNotificationStatus("SKIPPED");
        }

        documentService.save(document);

        // Actualizar datos del trabajador
        if (!document.isHistorical()) {
            employeeRepository.findById(document.getEmployeeId()).ifPresent(employee -> {
                boolean shouldUpdate = employee.getLatestFechaConcepto() == null ||
                        (fechaConcepto != null && !fechaConcepto.isBefore(employee.getLatestFechaConcepto()));

                if (shouldUpdate && fechaConcepto != null) {
                    employee.setLatestFechaConcepto(fechaConcepto);
                    employee.setLatestResultStatus(resultStatus);
                    employeeRepository.save(employee);
                }
            });
        }

        return buildResponse(document, analysis);
    }

    public Map<String, Object> analyzeDocument(String documentId) {
        return analyze(documentId);
    }   

    public Map<String, Object> backfillExtractedData() {
        List<ManagedDocument> documents = documentService.getAllDocuments();
        List<Map<String, Object>> updatedDocuments = new ArrayList<>();
        int checked = 0;
        int updated = 0;
        int failed = 0;

        for (ManagedDocument document : documents) {
            checked++;

            try {
                DocumentAnalysis currentAnalysis = documentAnalysisRepository
                        .findByDocumentId(document.getId())
                        .orElse(null);

                if (!needsExtractedDataBackfill(document, currentAnalysis)) {
                    continue;
                }

                Map<String, Object> result = refreshExtractedDataPreservingWorkflow(document, currentAnalysis);
                updatedDocuments.add(result);
                updated++;
            } catch (Exception ex) {
                failed++;
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("documentId", document.getId());
                failure.put("fileName", document.getOriginalFileName());
                failure.put("status", "ERROR");
                failure.put("message", ex.getMessage() != null ? ex.getMessage() : "No se pudo reprocesar.");
                updatedDocuments.add(failure);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("checked", checked);
        response.put("updated", updated);
        response.put("failed", failed);
        response.put("documents", updatedDocuments);

        return response;
    }

    private boolean needsExtractedDataBackfill(ManagedDocument document, DocumentAnalysis analysis) {
        if (document != null && document.getFechaConcepto() == null) {
            return true;
        }

        if (analysis == null) {
            return true;
        }

        if (analysis.getEvaluationDate() == null) {
            return true;
        }

        Map<String, Object> fields = analysis.getExtractedFields();

        return fields == null ||
                safeField(fields.get("birthDate")).isBlank() ||
                safeField(fields.get("fechaEvaluacion")).isBlank();
    }

    private Map<String, Object> refreshExtractedDataPreservingWorkflow(
            ManagedDocument document,
            DocumentAnalysis currentAnalysis
    ) {
        Path filePath = resolveFilePath(document);
        String extractedText = pdfTextExtractorService.extractText(filePath);
        String normalizedText = extractedText != null
                ? extractedText.toUpperCase().replaceAll("\\s+", " ").trim()
                : "";
        String resultStatus = resolveResultStatus(normalizedText);
        Map<String, Object> extractedFields = pdfFieldParserService.extractFields(extractedText);
        LocalDate fechaConcepto = resolveFechaConcepto(extractedFields);

        DocumentAnalysis analysis = currentAnalysis != null ? currentAnalysis : new DocumentAnalysis();
        analysis.setDocumentId(document.getId());
        analysis.setEmployeeId(document.getEmployeeId());
        analysis.setResultStatus(resultStatus);
        analysis.setExtractedFields(extractedFields);
        analysis.setExtractedText(extractedText);
        analysis.setEvaluationDate(fechaConcepto);
        analysis.setAnalyzedAt(Instant.now());
        documentAnalysisRepository.save(analysis);

        if (fechaConcepto != null) {
            document.setFechaConcepto(fechaConcepto);
            documentService.save(document);
        }

        if (!document.isHistorical()) {
            employeeRepository.findById(document.getEmployeeId()).ifPresent(employee -> {
                boolean shouldUpdate = fechaConcepto != null &&
                        (employee.getLatestFechaConcepto() == null ||
                                !fechaConcepto.isBefore(employee.getLatestFechaConcepto()));

                if (shouldUpdate) {
                    employee.setLatestFechaConcepto(fechaConcepto);
                    employee.setLatestResultStatus(resultStatus);
                    employeeRepository.save(employee);
                }
            });
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("documentId", document.getId());
        item.put("fileName", document.getOriginalFileName());
        item.put("status", "UPDATED");
        item.put("fechaEvaluacion", fechaConcepto);
        item.put("birthDate", extractedFields.get("birthDate"));
        item.put("resultStatus", resultStatus);

        return item;
    }

    public Map<String, Object> getSavedAnalysis(String documentId) {
        ManagedDocument document = documentService.findById(documentId);

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("El documento aun no tiene analisis guardado."));

        if (document.getFechaConcepto() == null && analysis.getEvaluationDate() == null) {
            refreshExtractedDataPreservingWorkflow(document, analysis);

            ManagedDocument refreshedDocument = documentService.findById(documentId);
            DocumentAnalysis refreshedAnalysis = documentAnalysisRepository.findByDocumentId(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("El documento aun no tiene analisis guardado."));

            return buildResponse(refreshedDocument, refreshedAnalysis);
        }

        return buildResponse(document, analysis);
    }

    public Map<String, Object> getAnalysis(String documentId) {
        return getSavedAnalysis(documentId);
    }


    private LocalDate resolveFechaConcepto(Map<String, Object> extractedFields) {
        if (extractedFields == null || extractedFields.isEmpty()) {
            return null;
        }

        String[] keys = {
                "fechaEvaluacion",
                "evaluationDate",
                "fechaConcepto",
                "conceptDate",
                "examDate",
                "fechaExamen",
                "date",
                "fecha"
        };

        for (String key : keys) {
            Object value = extractedFields.get(key);
            LocalDate parsed = parseDate(value != null ? String.valueOf(value) : "");

            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private String safeField(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private LocalDate parseDate(String value) {
        String raw = value == null ? "" : value.trim();

        if (raw.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato.
            }
        }

        return null;
    }

    private Path resolveFilePath(ManagedDocument document) {
        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            Path directPath = Paths.get(document.getFilePath());

            if (Files.exists(directPath)) {
                return directPath;
            }
        }

        if (document.getStoredFileName() == null || document.getStoredFileName().isBlank()) {
            throw new IllegalArgumentException("El documento no tiene ruta de archivo valida.");
        }

        Path fallbackPath = Paths.get(UPLOAD_DIR).resolve(document.getStoredFileName());

        if (!Files.exists(fallbackPath)) {
            throw new IllegalArgumentException("No se encontro el archivo PDF del documento.");
        }

        return fallbackPath;
    }

    private String resolveResultStatus(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return "PENDIENTE";
        }

        /*
         * Primero validar NO_APTO.
         * Importante: "NO APTO" contiene la palabra "APTO".
         * Por eso NO_APTO debe evaluarse antes que APTO.
         */
        if (normalizedText.contains("NO CUMPLE CON LOS REQUISITOS DE SALUD PARA TRABAJO EN ALTURAS")) {
            return "NO_APTO";
        }

        if (normalizedText.contains("NO CUMPLE CON LAS CONDICIONES DE SALUD PARA TRABAJO EN ALTURAS")) {
            return "NO_APTO";
        }

        if (normalizedText.contains("NO APTO")) {
            return "NO_APTO";
        }

        if (normalizedText.contains("NO_APTO")) {
            return "NO_APTO";
        }

        if (normalizedText.contains("CUMPLE CON LAS CONDICIONES DE SALUD PARA TRABAJO EN ALTURAS")) {
            return "APTO";
        }

        if (normalizedText.contains("CUMPLE CON LOS REQUISITOS DE SALUD PARA TRABAJO EN ALTURAS")) {
            return "APTO";
        }

        if (normalizedText.contains("APTO")) {
            return "APTO";
        }

        return "PENDIENTE";
    }

    private Map<String, Object> buildResponse(ManagedDocument document, DocumentAnalysis analysis) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("analysisId", analysis.getId());
        response.put("documentId", document.getId());
        response.put("employeeId", document.getEmployeeId());
        response.put("originalFileName", document.getOriginalFileName());
        response.put("documentType", document.getDocumentType());
        response.put("examType", document.getExamType());
        response.put("processingStatus", document.getProcessingStatus());
        response.put("reviewStatus", document.getReviewStatus());
        response.put("notificationStatus", document.getNotificationStatus());
        response.put("resultStatus", analysis.getResultStatus());
        response.put("fechaConcepto", document.getFechaConcepto());
        response.put("fechaEvaluacion", document.getFechaConcepto());
        response.put("evaluationDate", document.getFechaConcepto());
        response.put("extractedFields", analysis.getExtractedFields());
        response.put("extractedText", analysis.getExtractedText());
        response.put("analysisEvaluationDate", analysis.getEvaluationDate());
        response.put("analyzedAt", analysis.getAnalyzedAt());

        return response;
    }
}
