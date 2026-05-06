package com.backend.controller;

import com.backend.repository.EmailLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-logs")
@CrossOrigin
public class EmailLogController {
    private final EmailLogRepository repo;
    public EmailLogController(EmailLogRepository repo){this.repo=repo;}
    @GetMapping("/document/{documentId}") public ResponseEntity<?> byDoc(@PathVariable String documentId){ return ResponseEntity.ok(repo.findByDocumentId(documentId)); }
    @GetMapping("/batch/{batchId}") public ResponseEntity<?> byBatch(@PathVariable String batchId){ return ResponseEntity.ok(repo.findByBatchId(batchId)); }
}
