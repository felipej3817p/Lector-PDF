package com.backend.repository;

import com.backend.model.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserAuditLogRepository extends MongoRepository<UserAuditLog, String> {
    Page<UserAuditLog> findByUserId(String userId, Pageable pageable);
}
