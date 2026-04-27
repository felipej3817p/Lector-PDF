package com.backend.repository;

import com.backend.model.AreaCode;
import com.backend.model.ManagedDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface ManagedDocumentRepository extends MongoRepository<ManagedDocument, String> {
    List<ManagedDocument> findByEmployeeId(String employeeId);
    List<ManagedDocument> findByAreaCodeIn(Collection<AreaCode> areaCodes);
}