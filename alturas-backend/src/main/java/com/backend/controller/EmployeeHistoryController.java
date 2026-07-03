package com.backend.controller;

import com.backend.service.EmployeeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeHistoryController {

    private final EmployeeHistoryService employeeHistoryService;

    public EmployeeHistoryController(EmployeeHistoryService employeeHistoryService) {
        this.employeeHistoryService = employeeHistoryService;
    }

    @GetMapping("/{employeeId}/history")
    public ResponseEntity<List<Map<String, Object>>> getEmployeeHistory(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeHistoryService.getEmployeeHistory(employeeId));
    }
}