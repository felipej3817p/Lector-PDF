package com.backend.service;

import com.backend.model.User;
import com.backend.model.UserAuditLog;
import com.backend.repository.UserAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserAuditLogService {

    private final UserAuditLogRepository repository;
    private final MongoTemplate mongoTemplate;
    private final SystemSettingsService systemSettingsService;
    private volatile LocalDateTime lastPurgeAt;
    private volatile Integer lastPurgeRetentionMonths;

    public UserAuditLogService(
            UserAuditLogRepository repository,
            MongoTemplate mongoTemplate,
            SystemSettingsService systemSettingsService
    ) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.systemSettingsService = systemSettingsService;
    }

    public Page<UserAuditLog> getByUserId(String userId, int page, int size) {
        purgeExpiredLogsIfNeeded();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return repository.findByUserId(
                userId,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("_id"))
                )
        );
    }

    public Page<UserAuditLog> getAll(int page, int size) {
        purgeExpiredLogsIfNeeded();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return repository.findAll(
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("_id"))
                )
        );
    }

    public Page<UserAuditLog> search(
            int page,
            int size,
            String username,
            String modifiedBy,
            String action,
            String field,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        purgeExpiredLogsIfNeeded();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("_id"))
        );

        List<Criteria> criteria = new ArrayList<>();
        addContainsIgnoreCase(criteria, "username", username);
        addContainsIgnoreCase(criteria, "modifiedBy", modifiedBy);
        addExact(criteria, "action", action);
        addExact(criteria, "field", field);

        if (dateFrom != null || dateTo != null) {
            Criteria changedAt = Criteria.where("changedAt");
            if (dateFrom != null) changedAt = changedAt.gte(dateFrom);
            if (dateTo != null) changedAt = changedAt.lte(dateTo);
            criteria.add(changedAt);
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria));
        }

        long total = mongoTemplate.count(query, UserAuditLog.class);
        query.with(pageable);
        List<UserAuditLog> content = mongoTemplate.find(query, UserAuditLog.class);
        return new PageImpl<>(content, pageable, total);
    }

    private void addContainsIgnoreCase(List<Criteria> criteria, String field, String value) {
        if (value != null && !value.isBlank()) {
            criteria.add(Criteria.where(field).regex(java.util.regex.Pattern.quote(value.trim()), "i"));
        }
    }

    private void addExact(List<Criteria> criteria, String field, String value) {
        if (value != null && !value.isBlank()) {
            criteria.add(Criteria.where(field).is(value.trim()));
        }
    }

    public void logCreated(User user, String modifiedBy) {
        log(user, modifiedBy, "CREATED", "usuario", null, user.getUsername());
    }

    public void logDeleted(User user, String modifiedBy) {
        log(user, modifiedBy, "DELETED", "usuario", user.getUsername(), null);
    }

    public void logChange(User user, String modifiedBy, String action, String field, Object previousValue, Object newValue) {
        if (!Objects.equals(previousValue, newValue)) {
            log(user, modifiedBy, action, field, previousValue, newValue);
        }
    }

    private void log(User user, String modifiedBy, String action, String field, Object previousValue, Object newValue) {
        purgeExpiredLogsIfNeeded();
        UserAuditLog auditLog = new UserAuditLog();
        auditLog.setUserId(user.getId());
        auditLog.setUsername(user.getUsername());
        auditLog.setModifiedBy(modifiedBy);
        auditLog.setAction(action);
        auditLog.setField(field);
        auditLog.setPreviousValue(previousValue);
        auditLog.setNewValue(newValue);
        auditLog.setChangedAt(LocalDateTime.now());
        repository.save(auditLog);
    }

    @Scheduled(cron = "${app.audit.user-retention-cleanup-cron:0 15 2 * * *}", zone = "America/Bogota")
    public void purgeExpiredLogs() {
        int retentionMonths = systemSettingsService.getRuntimeSettings().getUserAuditRetentionMonths();
        int retentionSetting = retentionMonths;
        LocalDateTime limit;

        if (retentionMonths == 0) {
            limit = LocalDateTime.now().minusDays(1);
        } else {
            if (retentionMonths < 3) {
                retentionMonths = 12;
            }

            limit = LocalDateTime.now().minusMonths(retentionMonths);
        }

        Query query = new Query(Criteria.where("changedAt").lt(limit));
        mongoTemplate.remove(query, UserAuditLog.class);
        lastPurgeAt = LocalDateTime.now();
        lastPurgeRetentionMonths = retentionSetting;
    }

    private void purgeExpiredLogsIfNeeded() {
        LocalDateTime lastRun = lastPurgeAt;
        int currentRetentionMonths = systemSettingsService.getRuntimeSettings().getUserAuditRetentionMonths();

        if (
                lastRun != null &&
                        Objects.equals(lastPurgeRetentionMonths, currentRetentionMonths) &&
                        Duration.between(lastRun, LocalDateTime.now()).toMinutes() < 60
        ) {
            return;
        }

        purgeExpiredLogs();
    }
}
