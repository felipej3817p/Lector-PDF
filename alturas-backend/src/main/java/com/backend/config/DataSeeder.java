package com.backend.config;

import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername("admin").ifPresentOrElse(
                user -> {
                    if (!user.getRoles().contains(Role.SUPER_ADMIN)) {
                        user.setRoles(Set.of(Role.SUPER_ADMIN));
                        user.setAllowedAreas(Set.of());
                        userRepository.save(user);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setEmail("admin@local");
                    admin.setPassword(passwordEncoder.encode("Admin123*"));
                    admin.setRoles(Set.of(Role.SUPER_ADMIN));
                    admin.setAllowedAreas(Set.of());
                    admin.setEnabled(true);
                    userRepository.save(admin);
                }
        );
    }
}