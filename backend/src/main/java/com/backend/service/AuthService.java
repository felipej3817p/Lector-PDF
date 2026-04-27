package com.backend.service;

import com.backend.dto.auth.AuthResponse;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.RegisterRequest;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import com.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        Set<String> authorities = user.getRoles()
                .stream()
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getUsername(), authorities);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getAllowedAreas()
        );
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo ya existe.");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>(Set.of(Role.OPERADOR)));
        user.setAllowedAreas(
                request.getAllowedAreas() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getAllowedAreas())
        );
        user.setEnabled(true);

        User saved = userRepository.save(user);

        Set<String> authorities = saved.getRoles()
                .stream()
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(saved.getUsername(), authorities);

        return new AuthResponse(
                token,
                saved.getUsername(),
                saved.getEmail(),
                saved.getRoles(),
                saved.getAllowedAreas()
        );
    }
}