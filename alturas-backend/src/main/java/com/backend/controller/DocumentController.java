package com.backend.controller;

import com.backend.dto.document.DocumentReviewRequest;
import com.backend.dto.document.BulkReviewRequest;
import com.backend.model.HistoricalImportIssue;
import com.backend.service.EmailSendService;
import com.backend.model.ManagedDocument;
import com.backend.service.DocumentAnalysisService;
import com.backend.service.DocumentBatchService;
import com.backend.service.DocumentEmailTemplateService;
import com.backend.service.DocumentReportPdfService;
import com.backend.service.DocumentService;
import jakarta.validation.Valid;
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

    private static final int MAX_BATCH_FILES = 5;
    private static final long MAX_BATCH_BYTES = 50L * 1024L * 1024L;

    private final DocumentService documentService;
    private final DocumentAnalysisService documentAnalysisService;
    private final DocumentBatchService documentBatchService;
    private final DocumentReportPdfService documentReportPdfService;
    private final DocumentEmailTemplateService documentEmailTemplateService;
    private final EmailSendService emailSendService;

    public DocumentController(
            DocumentService documentService,
            DocumentAnalysisService documentAnalysisService,
            DocumentBatchService documentBatchService,
            DocumentReportPdfService documentReportPdfService,
            DocumentEmailTemplateService documentEmailTemplateService,
            EmailSendService emailSendService) {
        this.documentService = documentService;
        this.documentAnalysisService = documentAnalysisService;
        this.documentBatchService = documentBatchService;
        this.documentReportPdfService = documentReportPdfService;
        this.documentEmailTemplateService = documentEmailTemplateService;
        this.emailSendService = emailSendService;
    }

    @GetMapping
    public ResponseEntity<List<ManagedDocument>> getAll(
            @RequestParam(value = "historical", defaultValue = "true") boolean includeHistorical) {
        return ResponseEntity.ok(documentService.getAllDocuments(includeHistorical));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagedDocument> getById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ManagedDocument>> getByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(documentService.getDocumentsByEmployeeId(employeeId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ManagedDocument> upload(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("documentType") String documentType,
            @RequestParam("examType") String examType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String uploadedBy = authentication != null ? authentication.getName() : "system";

        ManagedDocument saved = documentService.uploadDocument(
                employeeId,
                documentType,
                examType,
                file,
                uploadedBy);

        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/upload/batch-auto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadBatchAuto(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "documentType", defaultValue = "CONCEPTO_MEDICO") String documentType,
            @RequestParam(value = "examType", defaultValue = "TRABAJO_EN_ALTURAS") String examType,
            @RequestParam(value = "uploadType", defaultValue = "REGULAR") String uploadType,
            Authentication authentication) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Debes adjuntar al menos un archivo PDF.");
        }

        boolean historical = "HISTORICAL".equals(uploadType);
        boolean isConstancia = "CONSTANCIA".equals(uploadType);

        if (!historical && !isConstancia && files.size() > MAX_BATCH_FILES) {
            throw new IllegalArgumentException("La carga normal analiza cada PDF. El limite seguro es "
                    + MAX_BATCH_FILES
                    + " PDFs por tanda. La app debe dividir la carpeta automaticamente; si ves este mensaje, vuelve a intentar con menos archivos.");
        }

        long totalBytes = files.stream().mapToLong(file -> file != null ? file.getSize() : 0L).sum();

        if (!historical && !isConstancia && totalBytes > MAX_BATCH_BYTES) {
            throw new IllegalArgumentException(
                    "La carga normal supera el tamano seguro por tanda. La app debe dividir la carpeta automaticamente; si ves este mensaje, intenta con menos PDFs o archivos mas livianos.");
        }

        String uploadedBy = authentication != null ? authentication.getName() : "system";

        Map<String, Object> response = documentBatchService.uploadAndAnalyze(
                files,
                documentType,
                examType,
                uploadedBy,
                uploadType);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/historical/issues")
    public ResponseEntity<List<HistoricalImportIssue>> getHistoricalImportIssues(
            @RequestParam(value = "batchId", required = false) String batchId) {
        if (batchId != null && !batchId.isBlank()) {
            return ResponseEntity.ok(documentBatchService.getHistoricalImportIssuesByBatch(batchId));
        }

        return ResponseEntity.ok(documentBatchService.getHistoricalImportIssues());
    }

    @GetMapping("/historical/issues/{id}/view")
    public ResponseEntity<org.springframework.core.io.Resource> viewHistoricalImportIssue(@PathVariable String id) {
        org.springframework.core.io.Resource resource = documentBatchService.getHistoricalImportIssueFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pdf-no-asociado-" + id + ".pdf\"")
                .body(resource);
    }

    @DeleteMapping("/historical/issues/all")
    public ResponseEntity<Void> deleteAllHistoricalImportIssuesExplicit() {
        documentBatchService.deleteAllHistoricalImportIssues();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/historical/issues/{id}")
    public ResponseEntity<Void> deleteHistoricalImportIssue(@PathVariable String id) {
        documentBatchService.deleteHistoricalImportIssue(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/historical/issues")
    public ResponseEntity<Void> deleteAllHistoricalImportIssues() {
        documentBatchService.deleteAllHistoricalImportIssues();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@PathVariable String id) {
        return ResponseEntity.ok(documentAnalysisService.analyze(id));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String id) {
        return ResponseEntity.ok(documentAnalysisService.getSavedAnalysis(id));
    }

    @PostMapping("/backfill-extracted-data")
    public ResponseEntity<Map<String, Object>> backfillExtractedData() {
        return ResponseEntity.ok(documentAnalysisService.backfillExtractedData());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ManagedDocument> approveAndNotify(
            @PathVariable String id,
            @Valid @RequestBody DocumentReviewRequest request) {
        return ResponseEntity.ok(documentService.approveAndNotify(id, request.getComment()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ManagedDocument> rejectReview(
            @PathVariable String id,
            @Valid @RequestBody DocumentReviewRequest request) {
        return ResponseEntity.ok(documentService.rejectReview(id, request.getComment()));
    }

    @PostMapping("/approve-bulk")
    public ResponseEntity<Map<String, Object>> approveBulk(@RequestBody BulkReviewRequest request) {
        return ResponseEntity.ok(documentService.approveBulk(request.getDocumentIds(), request.getComment()));
    }

    @PostMapping("/reject-bulk")
    public ResponseEntity<Map<String, Object>> rejectBulk(@RequestBody BulkReviewRequest request) {
        return ResponseEntity.ok(documentService.rejectBulk(request.getDocumentIds(), request.getComment()));
    }

    @PostMapping("/{id}/resend-email")
    public ResponseEntity<?> resendEmail(@PathVariable String id) {
        return ResponseEntity.ok(emailSendService.resendAnalysisEmail(id));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<org.springframework.core.io.Resource> viewDocument(@PathVariable String id) {
        org.springframework.core.io.Resource resource = documentService.getDocumentFile(id);
        ManagedDocument doc = documentService.getDocumentById(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getOriginalFileName() + "\"")
                .body(resource);
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
