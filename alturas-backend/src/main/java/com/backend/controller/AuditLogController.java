package com.backend.controller;

import com.backend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin
public class AuditLogController {
    private final AuditLogService service;
    public AuditLogController(AuditLogService service){this.service=service;}
    @GetMapping public ResponseEntity<?> getAll(){ return ResponseEntity.ok(service.getAll()); }
}
