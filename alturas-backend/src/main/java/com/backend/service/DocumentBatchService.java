package com.backend.service;

import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentBatchService {

    private final EmployeeRepository employeeRepository;
    private final DocumentService documentService;
    private final DocumentAnalysisService documentAnalysisService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final PdfFieldParserService pdfFieldParserService;

    public DocumentBatchService(
            EmployeeRepository employeeRepository,
            DocumentService documentService,
            DocumentAnalysisService documentAnalysisService,
            PdfTextExtractorService pdfTextExtractorService,
            PdfFieldParserService pdfFieldParserService
    ) {
        this.employeeRepository = employeeRepository;
        this.documentService = documentService;
        this.documentAnalysisService = documentAnalysisService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.pdfFieldParserService = pdfFieldParserService;
    }

    public List<Map<String, Object>> uploadAndAnalyze(
            List<MultipartFile> files,
            String documentType,
            String examType,
            String uploadedBy
    ) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return results;
        }

        for (MultipartFile file : files) {
            results.add(processOneFile(file, documentType, examType, uploadedBy));
        }

        return results;
    }

    private Map<String, Object> processOneFile(
            MultipartFile file,
            String documentType,
            String examType,
            String uploadedBy
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fileName", file != null ? file.getOriginalFilename() : "");

        try {
            validateFile(file);

            String documentNumber = extractDocumentNumber(file);

            if (documentNumber.isBlank()) {
                throw new IllegalArgumentException("No se encontró número de documento en el PDF.");
            }

            Employee employee = employeeRepository.findByDocumentNumber(documentNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe un trabajador con documento " + documentNumber + "."
                    ));

            ManagedDocument saved = documentService.uploadDocument(
                    employee.getId(),
                    documentType,
                    examType,
                    file,
                    uploadedBy
            );

            Map<String, Object> analysis = documentAnalysisService.analyzeDocument(saved.getId());

            item.put("status", "OK");
            item.put("message", "Cargado y analizado correctamente. Pendiente de revisión.");
            item.put("documentId", saved.getId());
            item.put("employeeId", employee.getId());
            item.put("employeeName", buildEmployeeName(employee));
            item.put("employeeDocument", documentNumber);
            item.put("areaCode", employee.getAreaCode() != null ? employee.getAreaCode().name() : "");
            item.put("resultStatus", analysis.get("resultStatus"));
            item.put("reviewStatus", saved.getReviewStatus() != null ? saved.getReviewStatus() : "PENDING_REVIEW");
            item.put("notificationStatus", saved.getNotificationStatus() != null ? saved.getNotificationStatus() : "NOT_PENDING");
        } catch (Exception ex) {
            item.put("status", "ERROR");
            item.put("message", ex.getMessage() != null ? ex.getMessage() : "Error procesando el archivo.");
            item.put("employeeName", "");
            item.put("employeeDocument", "");
            item.put("areaCode", "");
            item.put("resultStatus", "");
            item.put("reviewStatus", "");
            item.put("notificationStatus", "");
        }

        return item;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF.");
        }
    }

    private String extractDocumentNumber(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile("pdf-batch-", ".pdf");

        try {
            /*
             * No usar file.transferTo(...)
             * porque puede consumir/mover el archivo temporal del request.
             * Luego DocumentService no podría guardarlo.
             */
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String extractedText = pdfTextExtractorService.extractText(tempFile);
            Map<String, Object> fields = pdfFieldParserService.extractFields(extractedText);

            String documentNumber = String.valueOf(fields.getOrDefault("documentNumber", ""))
                    .replaceAll("\\D", "");

            return documentNumber.trim();
        } finally {
            Files.deleteIfExists(tempFile);
        }
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