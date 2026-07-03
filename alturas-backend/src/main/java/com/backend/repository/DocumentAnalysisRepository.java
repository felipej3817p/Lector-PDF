package com.backend.repository;

import com.backend.model.DocumentAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface DocumentAnalysisRepository extends MongoRepository<DocumentAnalysis, String> {

    Optional<DocumentAnalysis> findByDocumentId(String documentId);

    List<DocumentAnalysis> findByDocumentIdIn(Collection<String> documentIds);

    List<DocumentAnalysis> findByEmployeeId(String employeeId);
}
