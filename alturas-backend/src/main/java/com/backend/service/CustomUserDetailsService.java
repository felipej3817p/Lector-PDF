package com.backend.service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.backend.model.User;
import com.backend.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAccessEvaluator userAccessEvaluator;

    public CustomUserDetailsService(UserRepository userRepository, UserAccessEvaluator userAccessEvaluator) {
        this.userRepository = userRepository;
        this.userAccessEvaluator = userAccessEvaluator;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        LocalDateTime now = LocalDateTime.now();
        boolean accountStarted = user.getAccountStartDate() == null || !now.isBefore(user.getAccountStartDate());
        boolean accountNonExpired = user.getAccountExpirationDate() == null || now.isBefore(user.getAccountExpirationDate());
        boolean hasEffectiveRole = !userAccessEvaluator.effectiveRoles(user).isEmpty();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled() || !accountStarted || !hasEffectiveRole)
                .accountExpired(!accountNonExpired)
                .authorities(
                        userAccessEvaluator.effectiveRoles(user).stream()
                                .map(r -> "ROLE_" + r.name())
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toSet())
                )
                .build();
    }
}
