package com.backend.controller;

import com.backend.model.DocumentBatch;
import com.backend.service.DocumentBatchFacadeService;
import com.backend.service.EmailSendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/document-batches")
@CrossOrigin
public class DocumentBatchController {
    private final DocumentBatchFacadeService service; private final EmailSendService emailSendService;
    public DocumentBatchController(DocumentBatchFacadeService service, EmailSendService emailSendService){this.service=service;this.emailSendService=emailSendService;}
    @GetMapping public ResponseEntity<?> list(){ return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<DocumentBatch> get(@PathVariable String id){ return ResponseEntity.ok(service.findById(id)); }
    @GetMapping("/{id}/documents") public ResponseEntity<?> docs(@PathVariable String id){ return ResponseEntity.ok(service.getBatchDocuments(id)); }
    @GetMapping("/{id}/summary") public ResponseEntity<DocumentBatch> summary(@PathVariable String id){ return ResponseEntity.ok(service.findById(id)); }
    @PostMapping("/{id}/notify-approver") public ResponseEntity<?> notify(@PathVariable String id){ return ResponseEntity.ok(emailSendService.sendBatchSummaryEmail(id)); }
}
