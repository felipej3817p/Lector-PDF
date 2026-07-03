package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.model.UserAreaAssignment;
import com.backend.model.UserRoleAssignment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAccessEvaluator {

    public Set<Role> effectiveRoles(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        List<UserRoleAssignment> assignments = user.getRoleAssignments();

        if (assignments != null && !assignments.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            return assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getRole() != null)
                    .filter(UserRoleAssignment::isEnabled)
                    .filter(assignment -> isCurrentlyValid(assignment.getStartDate(), assignment.getEndDate(), now))
                    .map(UserRoleAssignment::getRole)
                    .collect(Collectors.toCollection(HashSet::new));
        }

        return user.getRoles() == null ? Collections.emptySet() : new HashSet<>(user.getRoles());
    }

    public Set<AreaCode> effectiveAllowedAreas(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        List<UserAreaAssignment> assignments = user.getAreaAssignments();

        if (assignments != null && !assignments.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            return assignments.stream()
                    .filter(assignment -> assignment != null && assignment.getAreaCode() != null)
                    .filter(UserAreaAssignment::isEnabled)
                    .filter(assignment -> isCurrentlyValid(assignment.getStartDate(), assignment.getEndDate(), now))
                    .map(UserAreaAssignment::getAreaCode)
                    .collect(Collectors.toCollection(HashSet::new));
        }

        return user.getAllowedAreas() == null ? Collections.emptySet() : new HashSet<>(user.getAllowedAreas());
    }

    public boolean hasRole(User user, Role role) {
        return effectiveRoles(user).contains(role);
    }

    public boolean hasAnyRole(User user) {
        return !effectiveRoles(user).isEmpty();
    }

    public String assignmentStatus(LocalDateTime startDate, LocalDateTime endDate, boolean enabled) {
        if (!enabled) {
            return "INACTIVO";
        }

        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && now.isBefore(startDate)) {
            return "PENDIENTE";
        }

        if (endDate != null && !now.isBefore(endDate)) {
            return "VENCIDO";
        }

        return "VIGENTE";
    }

    private boolean isCurrentlyValid(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime now) {
        boolean started = startDate == null || !now.isBefore(startDate);
        boolean notExpired = endDate == null || now.isBefore(endDate);
        return started && notExpired;
    }
}
