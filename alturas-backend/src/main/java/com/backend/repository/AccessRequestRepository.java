package com.backend.repository;

import com.backend.model.AccessRequest;
import com.backend.model.AreaCode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccessRequestRepository extends MongoRepository<AccessRequest, String> {

    List<AccessRequest> findByRequestedByUserIdOrderByCreatedAtDesc(String requestedByUserId);

    List<AccessRequest> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    Optional<AccessRequest> findByRequestedByUserIdAndRequestedAreaAndStatus(
            String requestedByUserId,
            AreaCode requestedArea,
            String status
    );
}