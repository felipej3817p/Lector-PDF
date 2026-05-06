package com.backend.repository;
import com.backend.model.DocumentBatch;import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface DocumentBatchRepository extends MongoRepository<DocumentBatch,String>{ List<DocumentBatch> findAllByOrderByUploadedAtDesc(); }
