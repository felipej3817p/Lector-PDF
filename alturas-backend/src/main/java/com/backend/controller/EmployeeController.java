package com.backend.controller;

import com.backend.dto.employee.EmployeeDashboardResponse;
import com.backend.dto.employee.EmployeeRequest;
import com.backend.dto.employee.EmployeeResponse;
import com.backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<EmployeeDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(employeeService.getEmployeeDashboard());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable String id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteBulkEmployees(@RequestBody List<String> ids) {
        employeeService.deleteBulkEmployees(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk/evaluations")
    public ResponseEntity<Void> deleteBulkEvaluations(@RequestBody List<String> ids) {
        employeeService.deleteBulkEvaluations(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk/historical")
    public ResponseEntity<Void> deleteBulkHistorical(@RequestBody List<String> ids) {
        employeeService.deleteBulkHistorical(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk/certificates")
    public ResponseEntity<Void> deleteBulkCertificates(@RequestBody List<String> ids) {
        employeeService.deleteBulkCertificates(ids);
        return ResponseEntity.noContent().build();
    }
}