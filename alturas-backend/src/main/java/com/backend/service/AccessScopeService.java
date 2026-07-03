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
    private final UserAccessEvaluator userAccessEvaluator;

    public AccessScopeService(UserRepository userRepository, UserAccessEvaluator userAccessEvaluator) {
        this.userRepository = userRepository;
        this.userAccessEvaluator = userAccessEvaluator;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado."));
    }

    public boolean hasRole(User user, Role role) {
        return user != null
                && userAccessEvaluator.hasRole(user, role);
    }

    public boolean isSuperAdmin(User user) {
        return hasRole(user, Role.SUPER_ADMIN) || hasRole(user, Role.ADMIN);
    }

    public boolean isApprover(User user) {
        return hasRole(user, Role.APROBADOR);
    }

    public boolean isOperator(User user) {
        return hasRole(user, Role.OPERADOR);
    }

    public boolean isViewer(User user) {
        return hasRole(user, Role.VISUALIZADOR);
    }

    public boolean hasGlobalDocumentAccess(User user) {
        return isSuperAdmin(user) || isApprover(user) || hasGlobalAreaAccess(user);
    }

    public boolean hasGlobalAreaAccess(User user) {
        return user != null
                && user.isGlobalAreaAccess()
                && (isOperator(user) || isViewer(user));
    }

    public boolean canReviewDocuments(User user) {
        return isSuperAdmin(user) || isApprover(user);
    }

    public boolean isIT(User user) {
        return hasRole(user, Role.IT);
    }

    public boolean canManageSettings(User user) {
        return isSuperAdmin(user) || isIT(user) || isApprover(user) || isOperator(user);
    }

    public boolean canOperateDocuments(User user) {
        return isSuperAdmin(user) || isOperator(user);
    }

    public boolean canWriteEmployees(User user) {
        return isSuperAdmin(user) || isOperator(user);
    }

    public boolean isReadOnlyViewer(User user) {
        return isViewer(user) && !isSuperAdmin(user) && !isApprover(user) && !isOperator(user);
    }

    public Set<AreaCode> getAllowedAreas(User user) {
        return user == null ? Collections.emptySet() : userAccessEvaluator.effectiveAllowedAreas(user);
    }

    public void assertSuperAdmin() {
        User currentUser = getCurrentUser();

        if (!isSuperAdmin(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos de administrador.");
        }
    }

    public void assertCanReviewDocuments() {
        User currentUser = getCurrentUser();

        if (!canReviewDocuments(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos para revisar, aprobar o rechazar documentos.");
        }
    }

    public void assertCanOperateDocuments() {
        User currentUser = getCurrentUser();

        if (!canOperateDocuments(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos para cargar, eliminar o modificar documentos.");
        }
    }

    public void assertCanWriteEmployees() {
        User currentUser = getCurrentUser();

        if (!canWriteEmployees(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos para crear, editar o eliminar trabajadores.");
        }
    }

    public void assertCanReadArea(AreaCode areaCode) {
        validateAreaAccess(areaCode);
    }

    public void validateAreaAccess(AreaCode areaCode) {
        User currentUser = getCurrentUser();

        if (hasGlobalDocumentAccess(currentUser)) {
            return;
        }

        if (areaCode == null || !getAllowedAreas(currentUser).contains(areaCode)) {
            throw new IllegalArgumentException("No tienes permiso para consultar esta zona.");
        }
    }

    public AreaCode resolveWritableArea(AreaCode requestedArea) {
        User currentUser = getCurrentUser();

        if (!canWriteEmployees(currentUser) && !canOperateDocuments(currentUser)) {
            throw new IllegalArgumentException("No tienes permisos de escritura sobre zonas.");
        }

        if (isSuperAdmin(currentUser) || hasGlobalAreaAccess(currentUser)) {
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
