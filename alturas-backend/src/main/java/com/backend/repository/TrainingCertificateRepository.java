package com.backend.repository;

import com.backend.model.TrainingCertificate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TrainingCertificateRepository extends MongoRepository<TrainingCertificate, String> {

    List<TrainingCertificate> findByEmployeeIdAndStatusOrderByUploadedAtDesc(String employeeId, String status);

    List<TrainingCertificate> findByEmployeeIdOrderByUploadedAtDesc(String employeeId);
}