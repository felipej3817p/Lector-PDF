package com.backend.service;

import com.backend.dto.employee.EmployeeDashboardResponse;
import com.backend.dto.employee.EmployeeRequest;
import com.backend.dto.employee.EmployeeResponse;
import com.backend.model.AreaCode;
import com.backend.model.DocumentAnalysis;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            AccessScopeService accessScopeService,
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
    }

    public List<EmployeeResponse> getAllEmployees() {
        User currentUser = accessScopeService.getCurrentUser();

        List<Employee> employees = accessScopeService.isSuperAdmin(currentUser)
                ? employeeRepository.findAll()
                : employeeRepository.findByAreaCodeIn(accessScopeService.getAllowedAreas(currentUser));

        return employees.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EmployeeDashboardResponse> getEmployeeDashboard() {
        User currentUser = accessScopeService.getCurrentUser();

        List<Employee> employees = accessScopeService.isSuperAdmin(currentUser)
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
        validateDocumentUnique(request.getDocumentNumber(), null);

        AreaCode resolvedArea = accessScopeService.resolveWritableArea(request.getAreaCode());

        Employee employee = new Employee();
        applyRequest(employee, request, resolvedArea);

        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updateEmployee(String id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        accessScopeService.validateAreaAccess(employee.getAreaCode());
        validateDocumentUnique(request.getDocumentNumber(), id);

        AreaCode resolvedArea = accessScopeService.resolveWritableArea(request.getAreaCode());
        applyRequest(employee, request, resolvedArea);

        return toResponse(employeeRepository.save(employee));
    }

    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada."));

        accessScopeService.validateAreaAccess(employee.getAreaCode());
        employeeRepository.delete(employee);
    }

    private EmployeeDashboardResponse toDashboardResponse(Employee employee) {
        List<ManagedDocument> documents = managedDocumentRepository
                .findByEmployeeIdOrderByUploadedAtDesc(employee.getId());

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
                lastDocument != null ? lastDocument.getId() : "",
                lastDocument != null ? lastDocument.getOriginalFileName() : "",
                lastDocument != null ? lastDocument.getUploadedAt() : null,
                lastDocument != null ? lastDocument.getProcessingStatus() : "",
                lastResultStatus
        );
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
        employee.setCurrentPosition(request.getCurrentPosition().trim());
        employee.setWorkArea(request.getWorkArea().trim());
        employee.setEmployer(safe(request.getEmployer()));
        employee.setArl(safe(request.getArl()));
        employee.setEmail(safe(request.getEmail()));
        employee.setZone(safe(request.getZone()));
        employee.setAreaCode(resolvedArea);
        employee.setActive(request.isActive());
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
                employee.getAreaCode(),
                employee.isActive()
        );
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