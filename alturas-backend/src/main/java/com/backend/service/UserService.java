package com.backend.service;

import com.backend.dto.user.CreateUserRequest;
import com.backend.dto.user.UpdateUserRequest;
import com.backend.dto.user.UserDto;
import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.model.UserAreaAssignment;
import com.backend.model.UserAuditLog;
import com.backend.model.UserRoleAssignment;
import com.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessScopeService accessScopeService;
    private final AuditLogService auditLogService;
    private final UserAuditLogService userAuditLogService;
    private final UserAccessEvaluator userAccessEvaluator;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessScopeService accessScopeService,
            AuditLogService auditLogService,
            UserAuditLogService userAuditLogService,
            UserAccessEvaluator userAccessEvaluator
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessScopeService = accessScopeService;
        this.auditLogService = auditLogService;
        this.userAuditLogService = userAuditLogService;
        this.userAccessEvaluator = userAccessEvaluator;
    }

    public List<UserDto> getAllUsers() {
        accessScopeService.assertSuperAdmin();

        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public UserDto getUserById(String id) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        return toDto(user);
    }

    public UserDto createUser(CreateUserRequest request) {
        accessScopeService.assertSuperAdmin();

        String username = safe(request.getUsername());
        String email = safe(request.getEmail());

        if (username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo ya existe.");
        }

        Set<Role> roles = request.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(request.getRoles());

        Set<AreaCode> allowedAreas = request.getAllowedAreas() == null
                ? new HashSet<>()
                : new HashSet<>(request.getAllowedAreas());

        String actor = accessScopeService.getCurrentUser().getUsername();
        List<UserRoleAssignment> roleAssignments = normalizeRoleAssignments(request.getRoleAssignments(), roles, actor);
        Set<Role> effectiveRoles = userAccessEvaluator.effectiveRoles(fromAssignments(roleAssignments, List.of(), request.isGlobalAreaAccess()));
        List<UserAreaAssignment> areaAssignments = normalizeAreaAssignments(request.getAreaAssignments(), allowedAreas, actor);
        Set<AreaCode> effectiveAreas = effectiveAreasFromAssignments(areaAssignments, allowedAreas);

        validateUserScope(rolesFromAssignments(roleAssignments), areasFromAssignments(areaAssignments, allowedAreas), request.isGlobalAreaAccess());
        validateValidity(request.getAccountStartDate(), request.getAccountExpirationDate());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoleAssignments(roleAssignments);
        user.setAreaAssignments(areaAssignments);
        user.setRoles(effectiveRoles);
        user.setAllowedAreas(resolveAllowedAreasForRoles(effectiveRoles, effectiveAreas));
        user.setGlobalAreaAccess(resolveGlobalAreaAccessForRoles(effectiveRoles, request.isGlobalAreaAccess()));
        user.setEnabled(request.isEnabled());
        user.setAccountStartDate(request.getAccountStartDate());
        user.setAccountExpirationDate(request.getAccountExpirationDate());
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEnabledChangedBy(actor);
        user.setEnabledChangedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.log("USER", saved.getId(), "CREATED", actor, "Usuario creado.", Map.of("enabled", saved.isEnabled()));
        userAuditLogService.logCreated(saved, actor);
        userAuditLogService.logChange(saved, actor, "ASSIGNED", "email", null, saved.getEmail());
        logInitialRoleAssignments(saved, actor);
        logInitialAreaAssignments(saved, actor);
        userAuditLogService.logChange(saved, actor, "ASSIGNED", "globalAreaAccess", null, saved.isGlobalAreaAccess());
        userAuditLogService.logChange(saved, actor, "ASSIGNED", "enabled", null, saved.isEnabled());
        userAuditLogService.logChange(saved, actor, "ASSIGNED", "fechaInicioAcceso", null, saved.getAccountStartDate());
        userAuditLogService.logChange(saved, actor, "ASSIGNED", "fechaFinAcceso", null, saved.getAccountExpirationDate());
        return toDto(saved);
    }

    public UserDto updateUser(String id, UpdateUserRequest request) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        String username = safe(request.getUsername());
        String email = safe(request.getEmail());

        if (username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }

        userRepository.findByUsername(username)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("El nombre de usuario ya existe.");
                });

        userRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("El correo ya existe.");
                });

        Set<Role> roles = request.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(request.getRoles());

        Set<AreaCode> allowedAreas = request.getAllowedAreas() == null
                ? new HashSet<>()
                : new HashSet<>(request.getAllowedAreas());
        String actor = accessScopeService.getCurrentUser().getUsername();
        List<UserRoleAssignment> roleAssignments = normalizeRoleAssignments(request.getRoleAssignments(), roles, actor);
        Set<Role> effectiveRoles = userAccessEvaluator.effectiveRoles(fromAssignments(roleAssignments, List.of(), request.isGlobalAreaAccess()));
        List<UserAreaAssignment> areaAssignments = normalizeAreaAssignments(request.getAreaAssignments(), allowedAreas, actor);
        Set<AreaCode> effectiveAreas = effectiveAreasFromAssignments(areaAssignments, allowedAreas);

        if (isBaseAdmin(user)
                && (!request.isEnabled()
                || request.getAccountStartDate() != null
                || request.getAccountExpirationDate() != null
                || !rolesFromAssignments(roleAssignments).contains(Role.SUPER_ADMIN)
                || !effectiveRoles.contains(Role.SUPER_ADMIN)
                || !"admin".equalsIgnoreCase(username))) {
            throw new IllegalArgumentException("El administrador base debe permanecer activo, sin vencimiento y con rol SUPER_ADMIN.");
        }

        validateUserScope(rolesFromAssignments(roleAssignments), areasFromAssignments(areaAssignments, allowedAreas), request.isGlobalAreaAccess());
        validateValidity(request.getAccountStartDate(), request.getAccountExpirationDate());

        String previousUsername = user.getUsername();
        String previousEmail = user.getEmail();
        List<UserRoleAssignment> previousRoleAssignments = user.getRoleAssignments() == null ? List.of() : List.copyOf(user.getRoleAssignments());
        List<UserAreaAssignment> previousAreaAssignments = user.getAreaAssignments() == null ? List.of() : List.copyOf(user.getAreaAssignments());
        boolean previousGlobalAreaAccess = user.isGlobalAreaAccess();
        boolean wasEnabled = user.isEnabled();
        LocalDateTime previousAccountStartDate = user.getAccountStartDate();
        LocalDateTime previousAccountExpirationDate = user.getAccountExpirationDate();
        user.setUsername(username);
        user.setEmail(email);
        user.setRoleAssignments(roleAssignments);
        user.setAreaAssignments(areaAssignments);
        user.setRoles(effectiveRoles);
        user.setAllowedAreas(resolveAllowedAreasForRoles(effectiveRoles, effectiveAreas));
        user.setGlobalAreaAccess(resolveGlobalAreaAccessForRoles(effectiveRoles, request.isGlobalAreaAccess()));
        user.setEnabled(request.isEnabled());
        user.setAccountStartDate(request.getAccountStartDate());
        user.setAccountExpirationDate(request.getAccountExpirationDate());
        user.setUpdatedBy(actor);
        user.setUpdatedAt(LocalDateTime.now());
        if (wasEnabled != user.isEnabled()) {
            user.setEnabledChangedBy(actor);
            user.setEnabledChangedAt(LocalDateTime.now());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        auditLogService.log("USER", saved.getId(), "UPDATED", actor, "Usuario actualizado.", Map.of("enabled", saved.isEnabled()));
        userAuditLogService.logChange(saved, actor, "UPDATED", "username", previousUsername, saved.getUsername());
        userAuditLogService.logChange(saved, actor, "UPDATED", "email", previousEmail, saved.getEmail());
        logRoleAssignmentChanges(saved, actor, previousRoleAssignments, saved.getRoleAssignments());
        logAreaAssignmentChanges(saved, actor, previousAreaAssignments, saved.getAreaAssignments());
        userAuditLogService.logChange(saved, actor, "UPDATED", "globalAreaAccess", previousGlobalAreaAccess, saved.isGlobalAreaAccess());
        userAuditLogService.logChange(saved, actor, saved.isEnabled() ? "ACTIVATED" : "DEACTIVATED", "enabled", wasEnabled, saved.isEnabled());
        userAuditLogService.logChange(saved, actor, "UPDATED", "fechaInicioAcceso", previousAccountStartDate, saved.getAccountStartDate());
        userAuditLogService.logChange(saved, actor, "UPDATED", "fechaFinAcceso", previousAccountExpirationDate, saved.getAccountExpirationDate());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            userAuditLogService.logChange(saved, actor, "UPDATED", "password", "********", "******** (actualizada)");
        }
        return toDto(saved);
    }

    public UserDto updateEnabled(String id, boolean enabled) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (isBaseAdmin(user) && !enabled) {
            throw new IllegalArgumentException("El administrador base debe permanecer activo.");
        }

        if (user.isEnabled() == enabled) {
            return toDto(user);
        }

        String actor = accessScopeService.getCurrentUser().getUsername();
        boolean previousEnabled = user.isEnabled();
        user.setEnabled(enabled);
        user.setEnabledChangedBy(actor);
        user.setEnabledChangedAt(LocalDateTime.now());
        user.setUpdatedBy(actor);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        String action = enabled ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log("USER", saved.getId(), action, actor, "Estado de usuario actualizado.", Map.of("enabled", enabled));
        userAuditLogService.logChange(saved, actor, action, "enabled", previousEnabled, enabled);
        return toDto(saved);
    }

    public String resetPasswordTemp(String id) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Generate simple password
        String tempPassword = "iniciar" + (100 + new java.util.Random().nextInt(900)) + "*";
        
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        
        String actor = accessScopeService.getCurrentUser().getUsername();
        user.setUpdatedBy(actor);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        auditLogService.log("USER", user.getId(), "UPDATED", actor, "Restablecimiento de contraseña temporal.", Map.of());
        userAuditLogService.logChange(user, actor, "UPDATED", "password", "********", "******** (temporal)");
        
        return tempPassword;
    }

    public Page<UserAuditLog> getAuditLogs(String id, int page, int size) {
        accessScopeService.assertSuperAdmin();

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }

        return userAuditLogService.getByUserId(id, page, size);
    }

    public Page<UserAuditLog> getAllAuditLogs(
            int page,
            int size,
            String username,
            String modifiedBy,
            String action,
            String field,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        accessScopeService.assertSuperAdmin();
        return userAuditLogService.search(page, size, username, modifiedBy, action, field, dateFrom, dateTo);
    }

    public void deleteUser(String id) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (isBaseAdmin(user)) {
            throw new IllegalArgumentException("No se puede eliminar el super administrador base.");
        }

        String actor = accessScopeService.getCurrentUser().getUsername();
        userAuditLogService.logDeleted(user, actor);
        userRepository.delete(user);
    }

    private void validateUserScope(Set<Role> roles, Set<AreaCode> allowedAreas, boolean globalAreaAccess) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debes asignar al menos un rol.");
        }

        /*
         * SUPER_ADMIN tiene acceso global.
         * No necesita áreas.
         */
        if (roles.contains(Role.SUPER_ADMIN) || roles.contains(Role.ADMIN)) {
            return;
        }

        /*
         * APROBADOR puede ver todos los documentos para revisión.
         * No necesita áreas.
         */
        if (roles.contains(Role.APROBADOR)) {
            return;
        }

        if (roles.contains(Role.IT)) {
            return;
        }

        /*
         * OPERADOR y VISUALIZADOR deben tener al menos un área.
         * VISUALIZADOR es solo lectura, pero también queda limitado por zonas.
         */
        if (!globalAreaAccess
                && (roles.contains(Role.OPERADOR) || roles.contains(Role.VISUALIZADOR))
                && (allowedAreas == null || allowedAreas.isEmpty())) {
            throw new IllegalArgumentException("El rol seleccionado debe tener al menos un área asignada.");
        }
    }

    private Set<AreaCode> resolveAllowedAreasForRoles(Set<Role> roles, Set<AreaCode> allowedAreas) {
        if (roles.contains(Role.SUPER_ADMIN) || roles.contains(Role.ADMIN) || roles.contains(Role.APROBADOR) || roles.contains(Role.IT)) {
            return new HashSet<>();
        }

        return allowedAreas == null ? new HashSet<>() : new HashSet<>(allowedAreas);
    }

    private boolean resolveGlobalAreaAccessForRoles(Set<Role> roles, boolean globalAreaAccess) {
        return roles.contains(Role.SUPER_ADMIN)
                || roles.contains(Role.ADMIN)
                || roles.contains(Role.APROBADOR)
                || (globalAreaAccess && (roles.contains(Role.OPERADOR) || roles.contains(Role.VISUALIZADOR)));
    }

    private List<UserRoleAssignment> normalizeRoleAssignments(
            List<UserRoleAssignment> incoming,
            Set<Role> fallbackRoles,
            String actor
    ) {
        List<UserRoleAssignment> source = incoming != null && !incoming.isEmpty()
                ? incoming
                : fallbackRoles == null
                        ? List.of()
                        : fallbackRoles.stream()
                                .map(role -> new UserRoleAssignment(role, null, null, true))
                                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<UserRoleAssignment> normalized = new java.util.ArrayList<>();

        for (UserRoleAssignment assignment : source) {
            if (assignment == null || assignment.getRole() == null) {
                continue;
            }

            validateValidity(assignment.getStartDate(), assignment.getEndDate());

            UserRoleAssignment next = new UserRoleAssignment();
            next.setRole(assignment.getRole());
            next.setStartDate(assignment.getStartDate());
            next.setEndDate(assignment.getEndDate());
            next.setEnabled(assignment.isEnabled());
            next.setCreatedBy(safe(assignment.getCreatedBy()).isBlank() ? actor : assignment.getCreatedBy());
            next.setCreatedAt(assignment.getCreatedAt() == null ? now : assignment.getCreatedAt());
            normalized.add(next);
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Debes asignar al menos un rol.");
        }

        return normalized;
    }

    private List<UserAreaAssignment> normalizeAreaAssignments(
            List<UserAreaAssignment> incoming,
            Set<AreaCode> fallbackAreas,
            String actor
    ) {
        List<UserAreaAssignment> source = incoming != null && !incoming.isEmpty()
                ? incoming
                : fallbackAreas == null
                        ? List.of()
                        : fallbackAreas.stream()
                                .map(area -> new UserAreaAssignment(area, null, null, true))
                                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<UserAreaAssignment> normalized = new java.util.ArrayList<>();

        for (UserAreaAssignment assignment : source) {
            if (assignment == null || assignment.getAreaCode() == null) {
                continue;
            }

            validateValidity(assignment.getStartDate(), assignment.getEndDate());

            UserAreaAssignment next = new UserAreaAssignment();
            next.setAreaCode(assignment.getAreaCode());
            next.setStartDate(assignment.getStartDate());
            next.setEndDate(assignment.getEndDate());
            next.setEnabled(assignment.isEnabled());
            next.setCreatedBy(safe(assignment.getCreatedBy()).isBlank() ? actor : assignment.getCreatedBy());
            next.setCreatedAt(assignment.getCreatedAt() == null ? now : assignment.getCreatedAt());
            normalized.add(next);
        }

        return normalized;
    }

    private User fromAssignments(
            List<UserRoleAssignment> roleAssignments,
            List<UserAreaAssignment> areaAssignments,
            boolean globalAreaAccess
    ) {
        User user = new User();
        user.setRoleAssignments(roleAssignments);
        user.setAreaAssignments(areaAssignments);
        user.setGlobalAreaAccess(globalAreaAccess);
        return user;
    }

    private Set<Role> rolesFromAssignments(List<UserRoleAssignment> assignments) {
        Set<Role> roles = new HashSet<>();

        if (assignments != null) {
            assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getRole() != null)
                    .map(UserRoleAssignment::getRole)
                    .forEach(roles::add);
        }

        return roles;
    }

    private Set<AreaCode> areasFromAssignments(List<UserAreaAssignment> assignments, Set<AreaCode> fallbackAreas) {
        Set<AreaCode> areas = new HashSet<>();

        if (assignments != null && !assignments.isEmpty()) {
            assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getAreaCode() != null)
                    .map(UserAreaAssignment::getAreaCode)
                    .forEach(areas::add);
            return areas;
        }

        return fallbackAreas == null ? areas : new HashSet<>(fallbackAreas);
    }

    private Set<AreaCode> effectiveAreasFromAssignments(List<UserAreaAssignment> assignments, Set<AreaCode> fallbackAreas) {
        if (assignments != null && !assignments.isEmpty()) {
            return userAccessEvaluator.effectiveAllowedAreas(fromAssignments(List.of(), assignments, false));
        }

        return fallbackAreas == null ? new HashSet<>() : new HashSet<>(fallbackAreas);
    }

    private UserDto toDto(User user) {
        Set<Role> effectiveRoles = userAccessEvaluator.effectiveRoles(user);
        Set<AreaCode> effectiveAllowedAreas = userAccessEvaluator.effectiveAllowedAreas(user);

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                effectiveRoles,
                user.getRoleAssignments(),
                effectiveRoles,
                effectiveAllowedAreas,
                user.getAreaAssignments(),
                effectiveAllowedAreas,
                user.isGlobalAreaAccess(),
                user.isEnabled(),
                user.getAccountStartDate(),
                user.getAccountExpirationDate(),
                user.getCreatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy(),
                user.getEnabledChangedBy(),
                user.getEnabledChangedAt(),
                user.getLastLoginAt(),
                user.isMustChangePassword()
        );
    }

    private void validateValidity(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("La fecha final de vigencia debe ser posterior a la fecha inicial.");
        }
    }

    private boolean isBaseAdmin(User user) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().contains(Role.SUPER_ADMIN)
                && "admin".equalsIgnoreCase(user.getUsername());
    }

    private void logInitialRoleAssignments(User user, String actor) {
        if (user.getRoleAssignments() == null) {
            return;
        }

        user.getRoleAssignments().stream()
                .filter(assignment -> assignment != null && assignment.getRole() != null)
                .forEach(assignment -> userAuditLogService.logChange(
                        user,
                        actor,
                        "ROLE_ASSIGNED",
                        "roleAssignments",
                        null,
                        roleAssignmentSummary(assignment)
                ));
    }

    private void logInitialAreaAssignments(User user, String actor) {
        if (user.getAreaAssignments() == null || user.isGlobalAreaAccess()) {
            return;
        }

        user.getAreaAssignments().stream()
                .filter(assignment -> assignment != null && assignment.getAreaCode() != null)
                .forEach(assignment -> userAuditLogService.logChange(
                        user,
                        actor,
                        "AREA_ASSIGNED",
                        "areaAssignments",
                        null,
                        areaAssignmentSummary(assignment)
                ));
    }

    private void logRoleAssignmentChanges(
            User user,
            String actor,
            List<UserRoleAssignment> previousAssignments,
            List<UserRoleAssignment> currentAssignments
    ) {
        Map<Role, UserRoleAssignment> previousByRole = roleAssignmentsByRole(previousAssignments);
        Map<Role, UserRoleAssignment> currentByRole = roleAssignmentsByRole(currentAssignments);

        previousByRole.forEach((role, previous) -> {
            UserRoleAssignment current = currentByRole.get(role);
            if (current == null) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        "ROLE_REMOVED",
                        "roleAssignments",
                        roleAssignmentSummary(previous),
                        null
                );
            }
        });

        currentByRole.forEach((role, current) -> {
            UserRoleAssignment previous = previousByRole.get(role);
            if (previous == null) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        "ROLE_ASSIGNED",
                        "roleAssignments",
                        null,
                        roleAssignmentSummary(current)
                );
                return;
            }

            String previousSummary = roleAssignmentSummary(previous);
            String currentSummary = roleAssignmentSummary(current);
            if (!Objects.equals(previousSummary, currentSummary)) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        roleAssignmentAction(previous, current),
                        "roleAssignments",
                        previousSummary,
                        currentSummary
                );
            }
        });
    }

    private void logAreaAssignmentChanges(
            User user,
            String actor,
            List<UserAreaAssignment> previousAssignments,
            List<UserAreaAssignment> currentAssignments
    ) {
        Map<AreaCode, UserAreaAssignment> previousByArea = areaAssignmentsByArea(previousAssignments);
        Map<AreaCode, UserAreaAssignment> currentByArea = areaAssignmentsByArea(currentAssignments);

        previousByArea.forEach((area, previous) -> {
            UserAreaAssignment current = currentByArea.get(area);
            if (current == null) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        "AREA_REMOVED",
                        "areaAssignments",
                        areaAssignmentSummary(previous),
                        null
                );
            }
        });

        currentByArea.forEach((area, current) -> {
            UserAreaAssignment previous = previousByArea.get(area);
            if (previous == null) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        "AREA_ASSIGNED",
                        "areaAssignments",
                        null,
                        areaAssignmentSummary(current)
                );
                return;
            }

            String previousSummary = areaAssignmentSummary(previous);
            String currentSummary = areaAssignmentSummary(current);
            if (!Objects.equals(previousSummary, currentSummary)) {
                userAuditLogService.logChange(
                        user,
                        actor,
                        areaAssignmentAction(previous, current),
                        "areaAssignments",
                        previousSummary,
                        currentSummary
                );
            }
        });
    }

    private Map<Role, UserRoleAssignment> roleAssignmentsByRole(List<UserRoleAssignment> assignments) {
        Map<Role, UserRoleAssignment> byRole = new LinkedHashMap<>();

        if (assignments != null) {
            assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getRole() != null)
                    .forEach(assignment -> byRole.put(assignment.getRole(), assignment));
        }

        return byRole;
    }

    private Map<AreaCode, UserAreaAssignment> areaAssignmentsByArea(List<UserAreaAssignment> assignments) {
        Map<AreaCode, UserAreaAssignment> byArea = new LinkedHashMap<>();

        if (assignments != null) {
            assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getAreaCode() != null)
                    .forEach(assignment -> byArea.put(assignment.getAreaCode(), assignment));
        }

        return byArea;
    }

    private String roleAssignmentAction(UserRoleAssignment previous, UserRoleAssignment current) {
        if (previous.isEnabled() != current.isEnabled()) {
            return current.isEnabled() ? "ROLE_ENABLED" : "ROLE_DISABLED";
        }

        return "ROLE_UPDATED";
    }

    private String areaAssignmentAction(UserAreaAssignment previous, UserAreaAssignment current) {
        if (previous.isEnabled() != current.isEnabled()) {
            return current.isEnabled() ? "AREA_ENABLED" : "AREA_DISABLED";
        }

        return "AREA_UPDATED";
    }

    private String roleAssignmentSummary(UserRoleAssignment assignment) {
        if (assignment == null || assignment.getRole() == null) {
            return "Rol sin información";
        }

        return "Rol " + roleLabel(assignment.getRole()) +
                " | Estado: " + (assignment.isEnabled() ? "habilitado" : "inhabilitado") +
                " | Desde: " + validityLabel(assignment.getStartDate(), "acceso inmediato") +
                " | Hasta: " + validityLabel(assignment.getEndDate(), "sin fecha límite");
    }

    private String areaAssignmentSummary(UserAreaAssignment assignment) {
        if (assignment == null || assignment.getAreaCode() == null) {
            return "Zona sin información";
        }

        return "Zona " + assignment.getAreaCode() +
                " | Estado: " + (assignment.isEnabled() ? "habilitada" : "inhabilitada") +
                " | Desde: " + validityLabel(assignment.getStartDate(), "acceso inmediato") +
                " | Hasta: " + validityLabel(assignment.getEndDate(), "sin fecha límite");
    }

    private String roleLabel(Role role) {
        return role == Role.SUPER_ADMIN ? "ADMIN" : role.name();
    }

    private String validityLabel(LocalDateTime value, String emptyText) {
        return value == null ? emptyText : value.toString().replace('T', ' ');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
