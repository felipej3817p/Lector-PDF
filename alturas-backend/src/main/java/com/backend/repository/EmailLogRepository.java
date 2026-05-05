package com.backend.repository;

import com.backend.model.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmailLogRepository extends MongoRepository<EmailLog, String> {

    boolean existsByDocumentIdAndResultStatusAndStatus(
            String documentId,
            String resultStatus,
            String status
    );

    List<EmailLog> findByDocumentId(String documentId);

    List<EmailLog> findByEmployeeId(String employeeId);
}