package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class AccessScopeService {

    private final UserRepository userRepository;

    public AccessScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado."));
    }

    public boolean isSuperAdmin(User user) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().contains(Role.SUPER_ADMIN);
    }

    public boolean isApprover(User user) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().contains(Role.APROBADOR);
    }

    public boolean hasGlobalDocumentAccess(User user) {
        return isSuperAdmin(user) || isApprover(user);
    }

    public boolean canReviewDocuments(User user) {
        return isSuperAdmin(user) || isApprover(user);
    }

    public Set<AreaCode> getAllowedAreas(User user) {
        if (user == null || user.getAllowedAreas() == null) {
            return Collections.emptySet();
        }
        return user.getAllowedAreas();
    }

    public void assertSuperAdmin() {
        User currentUser = getCurrentUser();

        if (!isSuperAdmin(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos de super administrador.");
        }
    }

    public void assertCanReviewDocuments() {
        User currentUser = getCurrentUser();

        if (!canReviewDocuments(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos para revisar, aprobar o rechazar documentos.");
        }
    }

    public void validateAreaAccess(AreaCode areaCode) {
        User currentUser = getCurrentUser();

        if (hasGlobalDocumentAccess(currentUser)) {
            return;
        }

        if (areaCode == null || !getAllowedAreas(currentUser).contains(areaCode)) {
            throw new IllegalArgumentException("No tienes permiso para operar sobre esta área.");
        }
    }

    public AreaCode resolveWritableArea(AreaCode requestedArea) {
        User currentUser = getCurrentUser();

        if (isSuperAdmin(currentUser)) {
            return requestedArea;
        }

        Set<AreaCode> allowedAreas = getAllowedAreas(currentUser);

        if (allowedAreas.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene áreas asignadas.");
        }

        if (requestedArea != null) {
            if (!allowedAreas.contains(requestedArea)) {
                throw new IllegalArgumentException("No tienes permiso para operar sobre esta área.");
            }
            return requestedArea;
        }

        if (allowedAreas.size() == 1) {
            return allowedAreas.iterator().next();
        }

        throw new IllegalArgumentException("Debes seleccionar un área permitida.");
    }
}