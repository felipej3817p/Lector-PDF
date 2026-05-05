package com.backend.controller;

import com.backend.model.ManagedDocument;
import com.backend.service.DocumentAnalysisService;
import com.backend.service.DocumentEmailTemplateService;
import com.backend.service.DocumentReportPdfService;
import com.backend.service.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAnalysisService documentAnalysisService;
    private final DocumentReportPdfService documentReportPdfService;
    private final DocumentEmailTemplateService documentEmailTemplateService;

    public DocumentController(
            DocumentService documentService,
            DocumentAnalysisService documentAnalysisService,
            DocumentReportPdfService documentReportPdfService,
            DocumentEmailTemplateService documentEmailTemplateService
    ) {
        this.documentService = documentService;
        this.documentAnalysisService = documentAnalysisService;
        this.documentReportPdfService = documentReportPdfService;
        this.documentEmailTemplateService = documentEmailTemplateService;
    }

    @GetMapping
    public ResponseEntity<List<ManagedDocument>> getAll() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagedDocument> getById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ManagedDocument> upload(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("documentType") String documentType,
            @RequestParam("examType") String examType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        String uploadedBy = authentication != null ? authentication.getName() : "system";

        ManagedDocument saved = documentService.uploadDocument(
                employeeId,
                documentType,
                examType,
                file,
                uploadedBy
        );

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/analyze")
    public ResponseEntity<?> analyze(@PathVariable String id) {
        return ResponseEntity.ok(documentAnalysisService.analyzeDocument(id));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<?> getAnalysis(@PathVariable String id) {
        return ResponseEntity.ok(documentAnalysisService.getSavedAnalysis(id));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<InputStreamResource> downloadReport(@PathVariable String id) {
        byte[] pdfBytes = documentReportPdfService.generateReport(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=document-report-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(new ByteArrayInputStream(pdfBytes)));
    }

    @GetMapping("/{id}/email-template")
    public ResponseEntity<Map<String, String>> getEmailTemplate(@PathVariable String id) {
        return ResponseEntity.ok(documentEmailTemplateService.buildTemplate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
} 