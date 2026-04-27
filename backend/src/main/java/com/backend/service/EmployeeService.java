package com.backend.service;

import com.backend.dto.employee.EmployeeRequest;
import com.backend.dto.employee.EmployeeResponse;
import com.backend.model.AreaCode;
import com.backend.model.Employee;
import com.backend.model.User;
import com.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccessScopeService accessScopeService;

    public EmployeeService(EmployeeRepository employeeRepository, AccessScopeService accessScopeService) {
        this.employeeRepository = employeeRepository;
        this.accessScopeService = accessScopeService;
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

    private void validateDocumentUnique(String documentNumber, String currentId) {
        employeeRepository.findByDocumentNumber(documentNumber)
                .filter(found -> currentId == null || !found.getId().equals(currentId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Ya existe una persona con ese número de documento.");
                });
    }

    private void applyRequest(Employee employee, EmployeeRequest request, AreaCode resolvedArea) {
        employee.setDocumentType(request.getDocumentType().trim());
        employee.setDocumentNumber(request.getDocumentNumber().trim());
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}