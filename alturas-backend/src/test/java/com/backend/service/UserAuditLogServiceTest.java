package com.backend.service;

import com.backend.model.SystemSettings;
import com.backend.model.UserAuditLog;
import com.backend.repository.UserAuditLogRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuditLogServiceTest {

    @Mock
    private UserAuditLogRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Test
    void purgeExpiredLogsUsesTwentyFourHoursWhenRetentionIsZero() {
        when(systemSettingsService.getRuntimeSettings()).thenReturn(settingsWithRetention(0));
        UserAuditLogService service = new UserAuditLogService(repository, mongoTemplate, systemSettingsService);

        LocalDateTime lowerBound = LocalDateTime.now().minusDays(1).minusSeconds(2);
        service.purgeExpiredLogs();
        LocalDateTime upperBound = LocalDateTime.now().minusDays(1).plusSeconds(2);

        LocalDateTime limit = captureChangedAtLimit();
        assertThat(limit).isAfterOrEqualTo(lowerBound);
        assertThat(limit).isBeforeOrEqualTo(upperBound);
    }

    @Test
    void purgeExpiredLogsRunsAgainWhenRetentionSettingChanges() {
        when(systemSettingsService.getRuntimeSettings()).thenReturn(
                settingsWithRetention(12),
                settingsWithRetention(0),
                settingsWithRetention(0)
        );
        when(repository.findAll(any(PageRequest.class))).thenReturn(Page.empty());
        UserAuditLogService service = new UserAuditLogService(repository, mongoTemplate, systemSettingsService);

        service.purgeExpiredLogs();
        service.getAll(0, 10);

        verify(mongoTemplate, org.mockito.Mockito.times(2)).remove(any(Query.class), eq(UserAuditLog.class));
    }

    private LocalDateTime captureChangedAtLimit() {
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(UserAuditLog.class));
        Document changedAt = queryCaptor.getValue().getQueryObject().get("changedAt", Document.class);
        return changedAt.get("$lt", LocalDateTime.class);
    }

    private SystemSettings settingsWithRetention(int retentionMonths) {
        SystemSettings settings = new SystemSettings();
        settings.setUserAuditRetentionMonths(retentionMonths);
        return settings;
    }
}
