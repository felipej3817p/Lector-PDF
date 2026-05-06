package com.backend.service;

import com.backend.model.AuditLog;
import com.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo){this.repo=repo;}
    public AuditLog log(String entityType,String entityId,String action,String by,String description, Map<String,Object> metadata){
        AuditLog l=new AuditLog(); l.setEntityType(entityType); l.setEntityId(entityId); l.setAction(action); l.setPerformedBy(by); l.setPerformedAt(LocalDateTime.now()); l.setDescription(description); l.setMetadata(metadata); return repo.save(l);
    }
    public List<AuditLog> getAll(){ return repo.findAll(); }
}
