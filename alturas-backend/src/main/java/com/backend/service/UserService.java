package com.backend.service;

import com.backend.dto.user.CreateUserRequest;
import com.backend.dto.user.UpdateUserRequest;
import com.backend.dto.user.UserDto;
import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessScopeService accessScopeService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessScopeService accessScopeService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessScopeService = accessScopeService;
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

        validateUserScope(roles, allowedAreas);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);
        user.setAllowedAreas(resolveAllowedAreasForRoles(roles, allowedAreas));
        user.setEnabled(request.isEnabled());

        return toDto(userRepository.save(user));
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

        validateUserScope(roles, allowedAreas);

        user.setUsername(username);
        user.setEmail(email);
        user.setRoles(roles);
        user.setAllowedAreas(resolveAllowedAreasForRoles(roles, allowedAreas));
        user.setEnabled(request.isEnabled());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toDto(userRepository.save(user));
    }

    public void deleteUser(String id) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (
                user.getRoles() != null
                        && user.getRoles().contains(Role.SUPER_ADMIN)
                        && "admin".equalsIgnoreCase(user.getUsername())
        ) {
            throw new IllegalArgumentException("No se puede eliminar el super administrador base.");
        }

        userRepository.delete(user);
    }

    private void validateUserScope(Set<Role> roles, Set<AreaCode> allowedAreas) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debes asignar al menos un rol.");
        }

        /*
         * SUPER_ADMIN tiene acceso global.
         * No necesita áreas.
         */
        if (roles.contains(Role.SUPER_ADMIN)) {
            return;
        }

        /*
         * APROBADOR puede ver todos los documentos para revisión.
         * No necesita áreas.
         */
        if (roles.contains(Role.APROBADOR)) {
            return;
        }

        /*
         * OPERADOR sí debe tener al menos un área.
         */
        if (roles.contains(Role.OPERADOR) && (allowedAreas == null || allowedAreas.isEmpty())) {
            throw new IllegalArgumentException("Un operador debe tener al menos un área asignada.");
        }
    }

    private Set<AreaCode> resolveAllowedAreasForRoles(Set<Role> roles, Set<AreaCode> allowedAreas) {
        if (roles.contains(Role.SUPER_ADMIN) || roles.contains(Role.APROBADOR)) {
            return new HashSet<>();
        }

        return allowedAreas == null ? new HashSet<>() : new HashSet<>(allowedAreas);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getAllowedAreas(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}