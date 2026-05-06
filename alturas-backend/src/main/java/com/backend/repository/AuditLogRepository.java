package com.backend.repository;
import com.backend.model.AuditLog;import org.springframework.data.mongodb.repository.MongoRepository;
public interface AuditLogRepository extends MongoRepository<AuditLog,String> {}
