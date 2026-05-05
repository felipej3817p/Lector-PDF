package com.backend.service;

import com.backend.dto.access.AccessRequestCreateRequest;
import com.backend.dto.access.AccessRequestReviewRequest;
import com.backend.model.AccessRequest;
import com.backend.model.AreaCode;
import com.backend.model.User;
import com.backend.repository.AccessRequestRepository;
import com.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccessRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final AccessScopeService accessScopeService;

    public AccessRequestService(
            AccessRequestRepository accessRequestRepository,
            UserRepository userRepository,
            AccessScopeService accessScopeService
    ) {
        this.accessRequestRepository = accessRequestRepository;
        this.userRepository = userRepository;
        this.accessScopeService = accessScopeService;
    }

    public AccessRequest createRequest(AccessRequestCreateRequest request) {
        User currentUser = accessScopeService.getCurrentUser();

        AreaCode requestedArea = request.getRequestedArea();

        if (currentUser.getAllowedAreas() != null && currentUser.getAllowedAreas().contains(requestedArea)) {
            throw new IllegalArgumentException("Ya tienes acceso a esta área.");
        }

        accessRequestRepository
                .findByRequestedByUserIdAndRequestedAreaAndStatus(currentUser.getId(), requestedArea, STATUS_PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya tienes una solicitud pendiente para esta área.");
                });

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setRequestedByUserId(currentUser.getId());
        accessRequest.setRequestedByUsername(currentUser.getUsername());
        accessRequest.setRequestedArea(requestedArea);
        accessRequest.setReason(safe(request.getReason()));
        accessRequest.setStatus(STATUS_PENDING);
        accessRequest.setCreatedAt(LocalDateTime.now());

        return accessRequestRepository.save(accessRequest);
    }

    public List<AccessRequest> getMyRequests() {
        User currentUser = accessScopeService.getCurrentUser();
        return accessRequestRepository.findByRequestedByUserIdOrderByCreatedAtDesc(currentUser.getId());
    }

    public List<AccessRequest> getPendingRequests() {
        accessScopeService.assertSuperAdmin();
        return accessRequestRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING);
    }

    public long countPendingRequests() {
        accessScopeService.assertSuperAdmin();
        return accessRequestRepository.countByStatus(STATUS_PENDING);
    }

    public AccessRequest approve(String id, AccessRequestReviewRequest request) {
        accessScopeService.assertSuperAdmin();

        User admin = accessScopeService.getCurrentUser();

        AccessRequest accessRequest = accessRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        if (!STATUS_PENDING.equals(accessRequest.getStatus())) {
            throw new IllegalArgumentException("La solicitud ya fue revisada.");
        }

        User targetUser = userRepository.findById(accessRequest.getRequestedByUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario solicitante no encontrado."));

        Set<AreaCode> allowedAreas = targetUser.getAllowedAreas() != null
                ? new HashSet<>(targetUser.getAllowedAreas())
                : new HashSet<>();

        allowedAreas.add(accessRequest.getRequestedArea());
        targetUser.setAllowedAreas(allowedAreas);
        userRepository.save(targetUser);

        accessRequest.setStatus(STATUS_APPROVED);
        accessRequest.setReviewedByUserId(admin.getId());
        accessRequest.setReviewedByUsername(admin.getUsername());
        accessRequest.setAdminComment(safe(request.getAdminComment()));
        accessRequest.setReviewedAt(LocalDateTime.now());

        return accessRequestRepository.save(accessRequest);
    }

    public AccessRequest reject(String id, AccessRequestReviewRequest request) {
        accessScopeService.assertSuperAdmin();

        User admin = accessScopeService.getCurrentUser();

        AccessRequest accessRequest = accessRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        if (!STATUS_PENDING.equals(accessRequest.getStatus())) {
            throw new IllegalArgumentException("La solicitud ya fue revisada.");
        }

        accessRequest.setStatus(STATUS_REJECTED);
        accessRequest.setReviewedByUserId(admin.getId());
        accessRequest.setReviewedByUsername(admin.getUsername());
        accessRequest.setAdminComment(safe(request.getAdminComment()));
        accessRequest.setReviewedAt(LocalDateTime.now());

        return accessRequestRepository.save(accessRequest);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}