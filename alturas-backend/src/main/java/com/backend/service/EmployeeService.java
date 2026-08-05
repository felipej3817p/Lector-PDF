package com.backend.service;

import com.backend.dto.employee.EmployeeDashboardResponse;
import com.backend.dto.employee.EmployeeRequest;
import com.backend.dto.employee.EmployeeResponse;
import com.backend.model.AreaCode;
import com.backend.model.DocumentAnalysis;
import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.TrainingCertificate;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmailLogRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import com.backend.repository.TrainingCertificateRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;
    private final TrainingCertificateRepository trainingCertificateRepository;
    private final EmailLogRepository emailLogRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService,
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            AuditLogService auditLogService,
            DocumentService documentService,
            TrainingCertificateRepository trainingCertificateRepository,
            EmailLogRepository emailLogRepository) {
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.auditLogService = auditLogService;
        this.documentService = documentService;
        this.trainingCertificateRepository = trainingCertificateRepository;
        this.emailLogRepository = emailLogRepository;
    }

    public List<EmployeeResponse> getAllEmployees() {
        User currentUser = accessScopeService.getCurrentUser();

        List<Employee> employees = canReadAllEmployees(currentUser)
                ? employeeRepository.findAll()
                : employeeRepository.findByAreaCodeIn(accessScopeService.getAllowedAreas(currentUser));

        return employees.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EmployeeDashboardResponse> getEmployeeDashboard() {
        User currentUser = accessScopeService.getCurrentUser();

        List<Employee> employees = canReadAllEmployees(currentUser)
                ? employeeRepository.findAll()
                : employeeRepository.findByAreaCodeIn(accessScopeService.getAllowedAreas(currentUser));

        return employees.stream()
                .sorted(Comparator.comparing(this::fullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDashboardResponse)
                .toList();
    }

    public EmployeeResponse getEmployeeById(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        accessScopeService.validateAreaAccess(employee.getAreaCode());

        return toResponse(employee);
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        accessScopeService.assertCanWriteEmployees();

        validateDocumentUnique(request.getDocumentNumber(), null);
        validateValidity(request.getActiveStartDate(), request.getActiveExpirationDate());

        AreaCode resolvedArea = request.getAreaCode() != null
                ? accessScopeService.resolveWritableArea(request.getAreaCode())
                : null;

        Employee employee = new Employee();
        applyRequest(employee, request, resolvedArea);
        String actor = accessScopeService.getCurrentUser().getUsername();
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setCreatedBy(actor);
        employee.setUpdatedAt(now);
        employee.setUpdatedBy(actor);
        Employee saved = employeeRepository.save(employee);
        auditLogService.log("EMPLOYEE", saved.getId(), "CREATED", actor, "Trabajador creado.", Map.of());
        return toResponse(saved);
    }

    public EmployeeResponse updateEmployee(String id, EmployeeRequest request) {
        accessScopeService.assertCanWriteEmployees();

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        accessScopeService.validateAreaAccess(employee.getAreaCode());
        validateDocumentUnique(request.getDocumentNumber(), id);
        validateValidity(request.getActiveStartDate(), request.getActiveExpirationDate());

        AreaCode resolvedArea = request.getAreaCode() != null
                ? accessScopeService.resolveWritableArea(request.getAreaCode())
                : null;
        boolean wasActive = employee.isActive();
        LocalDateTime previousStartDate = employee.getActiveStartDate();
        LocalDateTime previousExpirationDate = employee.getActiveExpirationDate();
        applyRequest(employee, request, resolvedArea);
        String actor = accessScopeService.getCurrentUser().getUsername();
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy(actor);
        if (wasActive != employee.isActive()
                || !Objects.equals(previousStartDate, employee.getActiveStartDate())
                || !Objects.equals(previousExpirationDate, employee.getActiveExpirationDate())) {
            employee.setStatusChangedAt(LocalDateTime.now());
            employee.setStatusChangedBy(actor);
        }
        Employee saved = employeeRepository.save(employee);
        auditLogService.log("EMPLOYEE", saved.getId(), "UPDATED", actor, "Trabajador actualizado.", Map.of("active", saved.isActive()));
        return toResponse(saved);
    }

    public void deleteEmployee(String id) {
        accessScopeService.assertCanWriteEmployees();

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        accessScopeService.validateAreaAccess(employee.getAreaCode());
        String actor = accessScopeService.getCurrentUser().getUsername();

        List<ManagedDocument> documents = managedDocumentRepository.findByEmployeeId(employee.getId());

        for (ManagedDocument document : documents) {
            documentService.deleteDocument(document.getId());
        }

        List<TrainingCertificate> certificates = trainingCertificateRepository.findByEmployeeIdOrderByUploadedAtDesc(employee.getId());
        certificates.forEach(this::deleteCertificateFileIfExists);
        if (!certificates.isEmpty()) {
            trainingCertificateRepository.deleteAll(certificates);
        }

        List<EmailLog> emailLogs = emailLogRepository.findByEmployeeId(employee.getId());
        if (!emailLogs.isEmpty()) {
            emailLogRepository.deleteAll(emailLogs);
        }

        employeeRepository.delete(employee);
        auditLogService.log(
                "EMPLOYEE",
                employee.getId(),
                "DELETED",
                actor,
                "Trabajador eliminado.",
                Map.of(
                        "documentosEliminados", documents.size(),
                        "constanciasEliminadas", certificates.size(),
                        "correosEliminados", emailLogs.size()
                )
        );
    }

    public void deleteBulkEmployees(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (String id : ids) {
            try {
                deleteEmployee(id);
            } catch (Exception ignored) {
            }
        }
    }

    public void deleteBulkEvaluations(List<String> ids) {
        accessScopeService.assertCanWriteEmployees();
        if (ids == null || ids.isEmpty()) return;
        String actor = accessScopeService.getCurrentUser().getUsername();

        for (String id : ids) {
            Employee employee = employeeRepository.findById(id).orElse(null);
            if (employee == null) continue;
            try {
                accessScopeService.validateAreaAccess(employee.getAreaCode());
            } catch (Exception e) {
                continue;
            }

            List<ManagedDocument> documents = managedDocumentRepository.findByEmployeeId(id);
            int count = 0;
            for (ManagedDocument doc : documents) {
                if (!doc.isHistorical()) {
                    documentService.deleteDocument(doc.getId());
                    count++;
                }
            }
            if (count > 0) {
                auditLogService.log("EMPLOYEE", id, "DELETED_EVALUATIONS", actor, "Evaluaciones eliminadas masivamente.", Map.of("count", count));
            }
        }
    }

    public void deleteBulkHistorical(List<String> ids) {
        accessScopeService.assertCanWriteEmployees();
        if (ids == null || ids.isEmpty()) return;
        String actor = accessScopeService.getCurrentUser().getUsername();

        for (String id : ids) {
            Employee employee = employeeRepository.findById(id).orElse(null);
            if (employee == null) continue;
            try {
                accessScopeService.validateAreaAccess(employee.getAreaCode());
            } catch (Exception e) {
                continue;
            }

            List<ManagedDocument> documents = managedDocumentRepository.findByEmployeeId(id);
            int count = 0;
            for (ManagedDocument doc : documents) {
                if (doc.isHistorical()) {
                    documentService.deleteDocument(doc.getId());
                    count++;
                }
            }
            if (count > 0) {
                auditLogService.log("EMPLOYEE", id, "DELETED_HISTORICAL", actor, "Historial eliminado masivamente.", Map.of("count", count));
            }
        }
    }

    public void deleteBulkCertificates(List<String> ids) {
        accessScopeService.assertCanWriteEmployees();
        if (ids == null || ids.isEmpty()) return;
        String actor = accessScopeService.getCurrentUser().getUsername();

        for (String id : ids) {
            Employee employee = employeeRepository.findById(id).orElse(null);
            if (employee == null) continue;
            try {
                accessScopeService.validateAreaAccess(employee.getAreaCode());
            } catch (Exception e) {
                continue;
            }

            List<TrainingCertificate> certificates = trainingCertificateRepository.findByEmployeeIdOrderByUploadedAtDesc(id);
            if (!certificates.isEmpty()) {
                certificates.forEach(this::deleteCertificateFileIfExists);
                trainingCertificateRepository.deleteAll(certificates);
                auditLogService.log("EMPLOYEE", id, "DELETED_CERTIFICATES", actor, "Constancias eliminadas masivamente.", Map.of("count", certificates.size()));
            }
        }
    }

    private void deleteCertificateFileIfExists(TrainingCertificate certificate) {
        if (certificate == null || safe(certificate.getFilePath()).isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(certificate.getFilePath()));
        } catch (IOException ignored) {
        }
    }

    private boolean canReadAllEmployees(User user) {
        return accessScopeService.isSuperAdmin(user)
                || accessScopeService.isApprover(user)
                || accessScopeService.hasGlobalAreaAccess(user);
    }

    private EmployeeDashboardResponse toDashboardResponse(Employee employee) {
        List<ManagedDocument> documents = managedDocumentRepository
                .findByEmployeeIdOrderByUploadedAtDesc(employee.getId()).stream()
                .filter(document -> document != null && !document.isHistorical())
                .toList();

        ManagedDocument lastDocument = documents.isEmpty() ? null : documents.get(0);

        String lastResultStatus = "";
        if (lastDocument != null) {
            Optional<DocumentAnalysis> analysis = documentAnalysisRepository.findByDocumentId(lastDocument.getId());
            lastResultStatus = analysis.map(DocumentAnalysis::getResultStatus).orElse("");
        }

        return new EmployeeDashboardResponse(
                employee.getId(),
                employee.getDocumentType(),
                employee.getDocumentNumber(),
                fullName(employee),
                employee.getAreaCode(),
                employee.getWorkArea(),
                employee.getCurrentPosition(),
                employee.getEmail(),
                employee.getZone(),
                employee.isActive(),
                employee.isCurrentlyActive(),
                employee.getActiveStartDate(),
                employee.getActiveExpirationDate(),
                lastDocument != null ? lastDocument.getId() : "",
                lastDocument != null ? lastDocument.getOriginalFileName() : "",
                lastDocument != null ? lastDocument.getUploadedAt() : null,
                lastDocument != null ? lastDocument.getFechaConcepto() : null,
                lastDocument != null ? lastDocument.getProcessingStatus() : "",
                lastResultStatus);
    }

    private void validateDocumentUnique(String documentNumber, String currentId) {
        String normalized = normalizeDocumentNumber(documentNumber);

        employeeRepository.findByDocumentNumber(normalized)
                .filter(found -> currentId == null || !found.getId().equals(currentId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Ya existe una persona con ese número de documento.");
                });
    }

    private void applyRequest(Employee employee, EmployeeRequest request, AreaCode resolvedArea) {
        employee.setDocumentType(request.getDocumentType().trim());
        employee.setDocumentNumber(normalizeDocumentNumber(request.getDocumentNumber()));
        employee.setFirstName(request.getFirstName().trim());
        employee.setSecondName(safe(request.getSecondName()));
        employee.setFirstLastName(request.getFirstLastName().trim());
        employee.setSecondLastName(safe(request.getSecondLastName()));
        employee.setGender(safe(request.getGender()));
        employee.setBirthDate(safe(request.getBirthDate()));
        employee.setCurrentPosition(safe(request.getCurrentPosition()));
        employee.setWorkArea(safe(request.getWorkArea()));
        employee.setEmployer(safe(request.getEmployer()));
        employee.setArl(safe(request.getArl()));
        employee.setEmail(safe(request.getEmail()));
        employee.setZone(safe(request.getZone()));
        employee.setEducationalLevel(safe(request.getEducationalLevel()));
        employee.setAreaCode(resolvedArea);
        employee.setActive(request.isActive());
        employee.setActiveStartDate(request.getActiveStartDate());
        employee.setActiveExpirationDate(request.getActiveExpirationDate());
    }

    private void validateValidity(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("La fecha final de vigencia debe ser posterior a la fecha inicial.");
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getDocumentType(),
                employee.getDocumentNumber(),
                employee.getFirstName(),
                employee.getSecondName(),
                employee.getFirstLastName(),
                employee.getSecondLastName(),
                employee.getGender(),
                employee.getBirthDate(),
                employee.getCurrentPosition(),
                employee.getWorkArea(),
                employee.getEmployer(),
                employee.getArl(),
                employee.getEmail(),
                employee.getZone(),
                employee.getEducationalLevel(),
                employee.getAreaCode(),
                employee.isActive(),
                employee.isCurrentlyActive(),
                employee.getActiveStartDate(),
                employee.getActiveExpirationDate(),
                employee.getCreatedAt(),
                employee.getCreatedBy(),
                employee.getUpdatedAt(),
                employee.getUpdatedBy(),
                employee.getStatusChangedAt(),
                employee.getStatusChangedBy(),
                employee.getLatestFechaConcepto(),
                employee.getLatestResultStatus());
    }

    private String fullName(Employee employee) {
        return String.join(" ",
                safe(employee.getFirstName()),
                safe(employee.getSecondName()),
                safe(employee.getFirstLastName()),
                safe(employee.getSecondLastName()))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeDocumentNumber(String value) {
        return value == null ? "" : value.replaceAll("\\D", "").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
