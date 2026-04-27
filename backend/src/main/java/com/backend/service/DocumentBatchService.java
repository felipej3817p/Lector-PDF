package com.backend.service;

import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
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

        for (MultipartFile file : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileName", file != null ? file.getOriginalFilename() : "");

            try {
                if (file == null || file.isEmpty()) {
                    throw new IllegalArgumentException("El archivo está vacío.");
                }

                String originalName = file.getOriginalFilename();
                if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                    throw new IllegalArgumentException("Solo se permiten archivos PDF.");
                }

                String documentNumber = extractDocumentNumber(file);
                if (documentNumber.isBlank()) {
                    throw new IllegalArgumentException("No se encontró número de documento en el PDF.");
                }

                Employee employee = employeeRepository.findByDocumentNumber(documentNumber)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No existe una persona con documento " + documentNumber + "."
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
                item.put("message", "Cargado y analizado correctamente.");
                item.put("documentId", saved.getId());
                item.put("employeeId", employee.getId());
                item.put("employeeDocument", documentNumber);
                item.put("resultStatus", analysis.get("resultStatus"));
            } catch (Exception ex) {
                item.put("status", "ERROR");
                item.put("message", ex.getMessage());
            }

            results.add(item);
        }

        return results;
    }

    private String extractDocumentNumber(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile("pdf-batch-", ".pdf");
        try {
            file.transferTo(tempFile.toFile());
            String extractedText = pdfTextExtractorService.extractText(tempFile);
            Map<String, Object> fields = pdfFieldParserService.extractFields(extractedText);
            String documentNumber = String.valueOf(fields.getOrDefault("documentNumber", ""))
                    .replaceAll("\\D", "");
            return documentNumber.trim();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
