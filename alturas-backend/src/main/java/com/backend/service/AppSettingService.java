package com.backend.service;

import com.backend.model.AppSetting;
import com.backend.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppSettingService {
    private final AppSettingRepository repository;
    private final Map<String, String> fallback;

    public AppSettingService(
            AppSettingRepository repository,
            @Value("${app.email.approver-to:}") String approverTo,
            @Value("${app.email.approver-cc:}") String approverCc,
            @Value("${app.email.worker-cc-hr:}") String workerHr,
            @Value("${app.email.worker-cc-payroll:}") String workerPayroll,
            @Value("${app.email.default-cc:}") String workerDefault
    ) {
        this.repository = repository;
        this.fallback = new HashMap<>();
        fallback.put("approver.to", safe(approverTo));
        fallback.put("approver.cc", safe(approverCc));
        fallback.put("worker.cc.hr", safe(workerHr));
        fallback.put("worker.cc.payroll", safe(workerPayroll));
        fallback.put("worker.cc.default", safe(workerDefault));
    }

    public List<AppSetting> getAll() { return repository.findAll(); }
    public String getValue(String key) { return repository.findByKey(key).map(AppSetting::getValue).filter(v -> !safe(v).isBlank()).orElse(safe(fallback.get(key))); }
    public AppSetting upsert(String key, String value, String category, String description, String updatedBy) {
        AppSetting setting = repository.findByKey(key).orElseGet(AppSetting::new);
        setting.setKey(key); setting.setValue(safe(value)); setting.setCategory(safe(category)); setting.setDescription(safe(description));
        setting.setUpdatedBy(safe(updatedBy)); setting.setUpdatedAt(LocalDateTime.now());
        return repository.save(setting);
    }
    public Map<String,String> getEmailSettings(){
        Map<String,String> out = new LinkedHashMap<>();
        List<String> keys = List.of("approver.to","approver.cc","worker.cc.hr","worker.cc.payroll","worker.cc.default");
        for(String k:keys) out.put(k,getValue(k));
        return out;
    }
    public Map<String,String> getZoneCoordinators(){
        Map<String,String> out = new LinkedHashMap<>();
        for(var s: repository.findByKeyStartingWith("zone.coordinator.")) out.put(s.getKey(), safe(s.getValue()));
        return out;
    }
    public List<String> getWorkerCcRecipients(String zone){
        Set<String> recipients = new LinkedHashSet<>();
        addCsv(recipients, getValue("worker.cc.hr")); addCsv(recipients, getValue("worker.cc.payroll")); addCsv(recipients, getValue("worker.cc.default"));
        if (!safe(zone).isBlank()) addCsv(recipients, getValue("zone.coordinator." + zone.trim().toUpperCase()));
        return new ArrayList<>(recipients);
    }
    private void addCsv(Set<String> set, String csv){ for(String s: safe(csv).split(",")){ if(!s.trim().isBlank()) set.add(s.trim()); }}
    private String safe(String v){ return Optional.ofNullable(v).orElse("").trim(); }
}
