package com.backend.controller;

import com.backend.model.TrainingCertificate;
import com.backend.service.TrainingCertificateService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class TrainingCertificateController {

    private final TrainingCertificateService trainingCertificateService;

    public TrainingCertificateController(TrainingCertificateService trainingCertificateService) {
        this.trainingCertificateService = trainingCertificateService;
    }

    @GetMapping("/api/employees/{employeeId}/certificates/eligibility")
    public ResponseEntity<Map<String, Object>> getCertificateEligibility(@PathVariable String employeeId) {
        return ResponseEntity.ok(trainingCertificateService.getCertificateEligibility(employeeId));
    }

    @PostMapping(
            value = "/api/employees/{employeeId}/certificates",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadCertificate(
            @PathVariable String employeeId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(trainingCertificateService.uploadCertificate(employeeId, file));
    }

    @GetMapping("/api/employees/{employeeId}/certificates")
    public ResponseEntity<List<Map<String, Object>>> listCertificates(@PathVariable String employeeId) {
        return ResponseEntity.ok(trainingCertificateService.listCertificates(employeeId));
    }

    @GetMapping("/api/certificates/{certificateId}/download")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable String certificateId) {
        TrainingCertificate certificate = trainingCertificateService.getCertificateForDownload(certificateId);
        Resource resource = trainingCertificateService.loadCertificateFile(certificate);

        String fileName = certificate.getOriginalFileName() != null
                ? certificate.getOriginalFileName().replace("\"", "")
                : "constancia.pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @DeleteMapping("/api/certificates/{certificateId}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable String certificateId) {
        trainingCertificateService.deleteCertificate(certificateId);
        return ResponseEntity.noContent().build();
    }
}