package com.backend.repository;

import com.backend.model.DocumentAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentAnalysisRepository extends MongoRepository<DocumentAnalysis, String> {
    Optional<DocumentAnalysis> findByDocumentId(String documentId);
}