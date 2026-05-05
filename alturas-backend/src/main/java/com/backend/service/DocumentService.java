package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.DocumentAnalysis;
import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final EmailSendService emailSendService;

    @Value("${app.storage.documents-dir:uploads/documents}")
    private String documentsDir;

    public DocumentService(
            ManagedDocumentRepository managedDocumentRepository,
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService,
            DocumentAnalysisRepository documentAnalysisRepository,
            EmailSendService emailSendService
    ) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.emailSendService = emailSendService;
    }

    public List<ManagedDocument> getAllDocuments() {
        User currentUser = accessScopeService.getCurrentUser();

        List<ManagedDocument> documents = accessScopeService.hasGlobalDocumentAccess(currentUser)
                ? managedDocumentRepository.findAll()
                : managedDocumentRepository.findByAreaCodeIn(accessScopeService.getAllowedAreas(currentUser));

        return documents.stream()
                .sorted(
                        Comparator.comparing(
                                ManagedDocument::getUploadedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .toList();
    }

    public ManagedDocument getDocumentById(String id) {
        ManagedDocument document = managedDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado."));

        accessScopeService.validateAreaAccess(document.getAreaCode());

        return document;
    }

    public ManagedDocument findById(String id) {
        return getDocumentById(id);
    }

    public ManagedDocument save(ManagedDocument document) {
        return managedDocumentRepository.save(document);
    }

    public ManagedDocument uploadDocument(
            String employeeId,
            String documentType,
            String examType,
            MultipartFile file,
            String uploadedBy
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes adjuntar un archivo PDF.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona asociada."));

        AreaCode resolvedArea = employee.getAreaCode();

        if (resolvedArea == null) {
            resolvedArea = accessScopeService.resolveWritableArea(null);
            employee.setAreaCode(resolvedArea);
            employeeRepository.save(employee);
        } else {
            accessScopeService.validateAreaAccess(resolvedArea);
        }

        try {
            Path uploadPath = Paths.get(documentsDir)
                    .resolve(sanitizePathSegment(uploadedBy))
                    .resolve(
                            employee.getDocumentNumber() != null
                                    ? sanitizePathSegment(employee.getDocumentNumber())
                                    : "sin_documento"
                    );

            Files.createDirectories(uploadPath);

            String safeOriginalName = sanitizeFileName(originalName);
            String storedFileName = UUID.randomUUID() + "_" + safeOriginalName;
            Path targetPath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath);

            ManagedDocument document = new ManagedDocument();
            document.setEmployeeId(employee.getId());
            document.setDocumentType(documentType);
            document.setExamType(examType);
            document.setOriginalFileName(originalName);
            document.setStoredFileName(storedFileName);
            document.setFilePath(targetPath.toString());
            document.setContentType(file.getContentType());
            document.setUploadedBy(uploadedBy);
            document.setUploadedAt(LocalDateTime.now());

            /*
             * Flujo formal:
             * El documento se carga, se analiza después y queda pendiente de revisión.
             */
            document.setProcessingStatus("UPLOADED");
            document.setReviewStatus("PENDING_REVIEW");
            document.setNotificationStatus("NOT_PENDING");

            document.setAreaCode(resolvedArea);

            return managedDocumentRepository.save(document);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar el archivo PDF.");
        }
    }

    public ManagedDocument approveAndNotify(String id, String comment) {
        /*
         * Pueden aprobar:
         * - SUPER_ADMIN
         * - APROBADOR
         */
        accessScopeService.assertCanReviewDocuments();

        User currentUser = accessScopeService.getCurrentUser();

        ManagedDocument document = getDocumentById(id);

        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(id)
                .orElseThrow(() -> new IllegalArgumentException("El documento debe tener análisis antes de aprobar."));

        String resultStatus = normalizeResultStatus(analysis.getResultStatus());

        if (!"APTO".equals(resultStatus) && !"NO_APTO".equals(resultStatus)) {
            throw new IllegalArgumentException("Solo se pueden aprobar documentos con resultado APTO o NO_APTO.");
        }

        document.setReviewStatus("APPROVED");
        document.setReviewedBy(currentUser.getUsername());
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewComment(safe(comment));
        document.setProcessingStatus("ANALYZED");

        EmailLog emailLog = emailSendService.sendAnalysisEmailIfEnabled(id);

        document.setNotificationStatus(emailLog.getStatus());

        if ("SENT".equals(emailLog.getStatus())) {
            document.setNotifiedAt(emailLog.getSentAt());
        }

        return managedDocumentRepository.save(document);
    }

    public ManagedDocument rejectReview(String id, String comment) {
        /*
         * Pueden rechazar:
         * - SUPER_ADMIN
         * - APROBADOR
         */
        accessScopeService.assertCanReviewDocuments();

        User currentUser = accessScopeService.getCurrentUser();

        ManagedDocument document = getDocumentById(id);

        document.setReviewStatus("REJECTED");
        document.setReviewedBy(currentUser.getUsername());
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewComment(safe(comment));
        document.setNotificationStatus("SKIPPED");

        return managedDocumentRepository.save(document);
    }

    public void deleteDocument(String id) {
        ManagedDocument document = managedDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado."));

        accessScopeService.validateAreaAccess(document.getAreaCode());

        try {
            if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            }
        } catch (IOException ignored) {
        }

        documentAnalysisRepository.findByDocumentId(id)
                .ifPresent(documentAnalysisRepository::delete);

        managedDocumentRepository.delete(document);
    }

    private String normalizeResultStatus(String resultStatus) {
        String value = safe(resultStatus).toUpperCase();

        if ("APTO".equals(value)) {
            return "APTO";
        }

        if ("NO_APTO".equals(value) || "NO APTO".equals(value)) {
            return "NO_APTO";
        }

        return "PENDIENTE";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizePathSegment(String value) {
        String sanitized = String.valueOf(value == null ? "" : value)
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");

        return sanitized.isBlank() ? "sin_valor" : sanitized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}