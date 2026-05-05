package com.backend.controller;

import com.backend.dto.access.AccessRequestCreateRequest;
import com.backend.dto.access.AccessRequestReviewRequest;
import com.backend.model.AccessRequest;
import com.backend.service.AccessRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access-requests")
@CrossOrigin
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping
    public ResponseEntity<AccessRequest> create(@Valid @RequestBody AccessRequestCreateRequest request) {
        return ResponseEntity.ok(accessRequestService.createRequest(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AccessRequest>> getMyRequests() {
        return ResponseEntity.ok(accessRequestService.getMyRequests());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AccessRequest>> getPendingRequests() {
        return ResponseEntity.ok(accessRequestService.getPendingRequests());
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> countPendingRequests() {
        return ResponseEntity.ok(Map.of("count", accessRequestService.countPendingRequests()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AccessRequest> approve(
            @PathVariable String id,
            @Valid @RequestBody AccessRequestReviewRequest request
    ) {
        return ResponseEntity.ok(accessRequestService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AccessRequest> reject(
            @PathVariable String id,
            @Valid @RequestBody AccessRequestReviewRequest request
    ) {
        return ResponseEntity.ok(accessRequestService.reject(id, request));
    }
}   