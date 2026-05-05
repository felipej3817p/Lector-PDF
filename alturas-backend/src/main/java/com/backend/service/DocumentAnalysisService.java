package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentAnalysisRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DocumentAnalysisService {

    private static final String UPLOAD_DIR = "uploads/documents";

    private final DocumentService documentService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final PdfFieldParserService pdfFieldParserService;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmailSendService emailSendService;

    public DocumentAnalysisService(
            DocumentService documentService,
            PdfTextExtractorService pdfTextExtractorService,
            PdfFieldParserService pdfFieldParserService,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmailSendService emailSendService
    ) {
        this.documentService = documentService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.pdfFieldParserService = pdfFieldParserService;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.emailSendService = emailSendService;
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

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseGet(DocumentAnalysis::new);

        analysis.setDocumentId(document.getId());
        analysis.setEmployeeId(document.getEmployeeId());
        analysis.setResultStatus(resultStatus);
        analysis.setExtractedFields(extractedFields);
        analysis.setExtractedText(extractedText);
        analysis.setAnalyzedAt(Instant.now());

        analysis = documentAnalysisRepository.save(analysis);
        
        if ("APTO".equals(resultStatus) || "NO_APTO".equals(resultStatus)) {
    document.setProcessingStatus("ANALYZED");
    document.setReviewStatus("PENDING_REVIEW");
    document.setNotificationStatus("NOT_PENDING");
} else {
    document.setProcessingStatus("PENDING_MANUAL_REVIEW");
    document.setReviewStatus("PENDING_REVIEW");
    document.setNotificationStatus("SKIPPED");
}

documentService.save(document);

        /*
         * Envío automático:
         * - Solo envía si app.email.auto-send-enabled=true
         * - EmailSendService internamente valida APTO / NO_APTO
         * - Si queda PENDIENTE, lo registra como SKIPPED
         * - Si falla SMTP, lo registra como FAILED
         * - Si envía, lo registra como SENT
         */
        try {
            emailSendService.sendAnalysisEmailIfEnabled(documentId);
        } catch (Exception ignored) {
            /*
             * El análisis no debe fallar por un error de correo.
             * Si EmailSendService alcanza a manejar el error, queda en email_logs.
             */
        }

        return buildResponse(document, analysis);
    }

    public Map<String, Object> analyzeDocument(String documentId) {
        return analyze(documentId);
    }

    public Map<String, Object> getSavedAnalysis(String documentId) {
        ManagedDocument document = documentService.findById(documentId);

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("El documento aún no tiene análisis guardado."));

        return buildResponse(document, analysis);
    }

    public Map<String, Object> getAnalysis(String documentId) {
        return getSavedAnalysis(documentId);
    }

    private Path resolveFilePath(ManagedDocument document) {
        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            Path directPath = Paths.get(document.getFilePath());
            if (Files.exists(directPath)) {
                return directPath;
            }
        }

        if (document.getStoredFileName() == null || document.getStoredFileName().isBlank()) {
            throw new IllegalArgumentException("El documento no tiene ruta de archivo válida.");
        }

        Path fallbackPath = Paths.get(UPLOAD_DIR).resolve(document.getStoredFileName());

        if (!Files.exists(fallbackPath)) {
            throw new IllegalArgumentException("No se encontró el archivo PDF del documento.");
        }

        return fallbackPath;
    }

    private String resolveResultStatus(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return "PENDIENTE";
        }

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
        response.put("resultStatus", analysis.getResultStatus());
        response.put("extractedFields", analysis.getExtractedFields());
        response.put("extractedText", analysis.getExtractedText());
        response.put("analyzedAt", analysis.getAnalyzedAt());
        return response;
    }
}