package com.backend.controller;

import com.backend.dto.user.CreateUserRequest;
import com.backend.dto.user.UpdateUserRequest;
import com.backend.dto.user.UpdateUserStatusRequest;
import com.backend.dto.user.UserDto;
import com.backend.model.UserAuditLog;
import com.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<UserDto> updateEnabled(
            @PathVariable String id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateEnabled(id, request.isEnabled()));
    }

    @PostMapping("/{id}/reset-password-temp")
    public ResponseEntity<java.util.Map<String, String>> resetPasswordTemp(@PathVariable String id) {
        String tempPassword = userService.resetPasswordTemp(id);
        return ResponseEntity.ok(java.util.Map.of("tempPassword", tempPassword));
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<Page<UserAuditLog>> getAuditLogs(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.getAuditLogs(id, page, size));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<UserAuditLog>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String modifiedBy,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        return ResponseEntity.ok(userService.getAllAuditLogs(
                page, size, username, modifiedBy, action, field, dateFrom, dateTo
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
