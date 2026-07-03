package com.backend.controller;

import com.backend.repository.EmailLogRepository;
import com.backend.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-logs")
@CrossOrigin
public class EmailLogController {

    private final EmailLogRepository repo;
    private final DocumentService documentService;

    public EmailLogController(EmailLogRepository repo, DocumentService documentService) {
        this.repo = repo;
        this.documentService = documentService;
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<?> byDoc(@PathVariable String documentId) {
        /*
         * Valida permisos por zona antes de mostrar trazabilidad.
         * VISUALIZADOR solo puede ver logs de documentos pertenecientes a sus zonas.
         */
        documentService.getDocumentById(documentId);

        return ResponseEntity.ok(repo.findByDocumentIdOrderByCreatedAtDesc(documentId));
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<?> byBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(repo.findByBatchId(batchId));
    }
}