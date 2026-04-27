package com.backend.service;

import com.backend.dto.user.CreateUserRequest;
import com.backend.dto.user.UpdateUserRequest;
import com.backend.dto.user.UserDto;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

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

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo ya existe.");
        }

        validateUserScope(request.getRoles(), request.getAllowedAreas());

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(request.getRoles() == null ? new HashSet<>() : new HashSet<>(request.getRoles()));
        user.setAllowedAreas(request.getAllowedAreas() == null ? new HashSet<>() : new HashSet<>(request.getAllowedAreas()));
        user.setEnabled(request.isEnabled());

        return toDto(userRepository.save(user));
    }

    public UserDto updateUser(String id, UpdateUserRequest request) {
        accessScopeService.assertSuperAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        userRepository.findByUsername(request.getUsername())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("El nombre de usuario ya existe.");
                });

        userRepository.findByEmail(request.getEmail())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("El correo ya existe.");
                });

        validateUserScope(request.getRoles(), request.getAllowedAreas());

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setRoles(request.getRoles() == null ? new HashSet<>() : new HashSet<>(request.getRoles()));
        user.setAllowedAreas(request.getAllowedAreas() == null ? new HashSet<>() : new HashSet<>(request.getAllowedAreas()));
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

        if (user.getRoles() != null && user.getRoles().contains(Role.SUPER_ADMIN) && "admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("No se puede eliminar el super administrador base.");
        }

        userRepository.delete(user);
    }

    private void validateUserScope(java.util.Set<Role> roles, java.util.Set<com.backend.model.AreaCode> allowedAreas) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debes asignar al menos un rol.");
        }

        if (roles.contains(Role.SUPER_ADMIN)) {
            return;
        }

        if (roles.contains(Role.OPERADOR) && (allowedAreas == null || allowedAreas.isEmpty())) {
            throw new IllegalArgumentException("Un operador debe tener al menos un área asignada.");
        }
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
}