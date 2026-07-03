package com.backend.repository;

import com.backend.model.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Collection;

public interface EmailLogRepository extends MongoRepository<EmailLog, String> {

    boolean existsByDocumentIdAndResultStatusAndStatus(
            String documentId,
            String resultStatus,
            String status
    );

    List<EmailLog> findByDocumentId(String documentId);

    List<EmailLog> findByDocumentIdOrderByCreatedAtDesc(String documentId);

    List<EmailLog> findByDocumentIdIn(Collection<String> documentIds);

    List<EmailLog> findByEmployeeId(String employeeId);
    List<EmailLog> findByBatchId(String batchId);
}
