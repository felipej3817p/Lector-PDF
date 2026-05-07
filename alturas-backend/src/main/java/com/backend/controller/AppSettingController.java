package com.backend.controller;

import com.backend.model.AppSetting;
import com.backend.service.AppSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin
public class AppSettingController {
    private final AppSettingService service;
    public AppSettingController(AppSettingService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<AppSetting>> all(){ return ResponseEntity.ok(service.getAll()); }
    @GetMapping("/{key}") public ResponseEntity<Map<String,String>> getKey(@PathVariable String key){ return ResponseEntity.ok(Map.of("key", key, "value", service.getValue(key))); }
    @PutMapping("/{key}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AppSetting> putKey(@PathVariable String key, @RequestBody Map<String,String> body, Authentication auth){
        return ResponseEntity.ok(service.upsert(key, body.get("value"), body.getOrDefault("category","GENERAL"), body.getOrDefault("description",""), auth != null ? auth.getName() : "system"));
    }
    @GetMapping("/email") public ResponseEntity<Map<String,String>> email(){ return ResponseEntity.ok(service.getEmailSettings()); }
    @PutMapping("/email") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> putEmail(@RequestBody Map<String,String> payload, Authentication auth){
        String user = auth != null ? auth.getName() : "system";
        for (var e: payload.entrySet()) service.upsert(e.getKey(), e.getValue(), "EMAIL", "Configuración de correo", user);
        return ResponseEntity.ok(service.getEmailSettings());
    }
    @GetMapping("/zone-coordinators") public ResponseEntity<Map<String,String>> zones(){ return ResponseEntity.ok(service.getZoneCoordinators()); }
    @PutMapping("/zone-coordinators") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> putZones(@RequestBody Map<String,String> payload, Authentication auth){
        String user = auth != null ? auth.getName() : "system";
        for (var e: payload.entrySet()) service.upsert(e.getKey(), e.getValue(), "ZONE_COORDINATOR", "Coordinadores por zona", user);
        return ResponseEntity.ok(service.getZoneCoordinators());
    }
}
