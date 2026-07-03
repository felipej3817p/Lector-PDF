package com.backend.repository;

import com.backend.model.HistoricalImportIssue;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistoricalImportIssueRepository extends MongoRepository<HistoricalImportIssue, String> {
    List<HistoricalImportIssue> findAllByOrderByCreatedAtDesc();
    List<HistoricalImportIssue> findByBatchIdOrderByCreatedAtDesc(String batchId);
}
