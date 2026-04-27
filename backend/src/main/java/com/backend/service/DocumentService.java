package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
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
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;

    @Value("${app.storage.documents-dir:uploads/documents}")
    private String documentsDir;

    public DocumentService(
            ManagedDocumentRepository managedDocumentRepository,
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService
    ) {
        this.managedDocumentRepository = managedDocumentRepository;
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
    }

    public List<ManagedDocument> getAllDocuments() {
        User currentUser = accessScopeService.getCurrentUser();

        return accessScopeService.isSuperAdmin(currentUser)
                ? managedDocumentRepository.findAll().stream()
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .toList()
                : managedDocumentRepository.findByAreaCodeIn(accessScopeService.getAllowedAreas(currentUser)).stream()
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
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
                    .resolve(employee.getDocumentNumber() != null ? sanitizePathSegment(employee.getDocumentNumber()) : "sin_documento");
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
            document.setProcessingStatus("UPLOADED");
            document.setAreaCode(resolvedArea);

            return managedDocumentRepository.save(document);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar el archivo PDF.");
        }
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

        managedDocumentRepository.delete(document);
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
}
