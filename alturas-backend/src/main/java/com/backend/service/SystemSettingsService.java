package com.backend.service;

import com.backend.dto.settings.SystemSettingsRequest;
import com.backend.model.AreaCode;
import com.backend.model.SystemSettings;
import com.backend.repository.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository repository;
    private final AccessScopeService accessScopeService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String fallbackFrontendBaseUrl;

    @Value("${app.email.approver-to:}")
    private String fallbackApproverEmails;

    @Value("${app.email.approver-cc:}")
    private String fallbackApproverCc;

    @Value("${app.email.default-cc:}")
    private String fallbackDefaultWorkerCc;

    public SystemSettingsService(
            SystemSettingsRepository repository,
            AccessScopeService accessScopeService
    ) {
        this.repository = repository;
        this.accessScopeService = accessScopeService;
    }

    /*
     * Endpoint administrativo.
     * Solo roles autorizados pueden ver y modificar estos correos desde la app.
     */
    public SystemSettings getSettingsForAdmin() {
        if (!accessScopeService.canManageSettings(accessScopeService.getCurrentUser())) {
            throw new IllegalArgumentException("No tienes permisos para gestionar la configuración.");
        }
        return getOrCreateSettings();
    }

    /*
     * Endpoint administrativo.
     * Guarda en MongoDB lo que se configure en la pantalla Configuración > Correos.
     */
    public SystemSettings updateSettings(SystemSettingsRequest request) {
        if (!accessScopeService.canManageSettings(accessScopeService.getCurrentUser())) {
            throw new IllegalArgumentException("No tienes permisos para modificar la configuración.");
        }

        SystemSettings settings = getOrCreateSettings();

        if (request.getAutoSendApproverEmail() != null) {
            settings.setAutoSendApproverEmail(request.getAutoSendApproverEmail());
        }

        if (request.getUserAuditRetentionMonths() != null) {
            if (!accessScopeService.isSuperAdmin(accessScopeService.getCurrentUser())) {
                throw new IllegalArgumentException("Solo ADMIN puede modificar la retencion de auditoria.");
            }

            settings.setUserAuditRetentionMonths(normalizeAuditRetentionMonths(request.getUserAuditRetentionMonths()));
        }

        if (request.getFrontendBaseUrl() != null) {
            settings.setFrontendBaseUrl(normalizeText(request.getFrontendBaseUrl()));
        }

        if (request.getApproverEmails() != null) {
            settings.setApproverEmails(normalizeEmailList(request.getApproverEmails()));
        }

        if (request.getApproverCc() != null) {
            settings.setApproverCc(normalizeEmailList(request.getApproverCc()));
        }

        if (request.getHumanTalentEmails() != null) {
            settings.setHumanTalentEmails(normalizeEmailList(request.getHumanTalentEmails()));
        }

        if (request.getPayrollEmails() != null) {
            settings.setPayrollEmails(normalizeEmailList(request.getPayrollEmails()));
        }

        if (request.getZoneCoordinatorEmails() != null) {
            settings.setZoneCoordinatorEmails(normalizeZoneCoordinators(request.getZoneCoordinatorEmails()));
        }
        settings.setUpdatedAt(LocalDateTime.now());

        return repository.save(settings);
    }

    /*
     * Uso interno del backend.
     * No exige usuario autenticado porque también se usa durante el flujo de carga/envío.
     */
    public SystemSettings getRuntimeSettings() {
        return getOrCreateSettings();
    }

    public String resolveFrontendBaseUrl(SystemSettings settings) {
        String fromSettings = safe(settings.getFrontendBaseUrl());
        String fallback = safe(fallbackFrontendBaseUrl);

        if (!fromSettings.isBlank() && !isOutdatedFrontendUrl(fromSettings)) {
            return fromSettings;
        }

        return fallback;
    }

    public String resolveApproverEmails(SystemSettings settings) {
        String fromSettings = safe(settings.getApproverEmails());
        return !fromSettings.isBlank() ? fromSettings : safe(fallbackApproverEmails);
    }

    public String resolveApproverCc(SystemSettings settings) {
        String fromSettings = safe(settings.getApproverCc());
        return !fromSettings.isBlank() ? fromSettings : safe(fallbackApproverCc);
    }

    public String resolveWorkerCc(SystemSettings settings, AreaCode areaCode) {
        String humanTalent = safe(settings.getHumanTalentEmails());
        String payroll = safe(settings.getPayrollEmails());
        String coordinator = "";

        if (settings.getZoneCoordinatorEmails() != null && areaCode != null) {
            String areaKey = canonicalAreaKey(areaCode.name());
            coordinator = safe(settings.getZoneCoordinatorEmails().get(areaKey));

            if (coordinator.isBlank()) {
                coordinator = safe(settings.getZoneCoordinatorEmails().get(areaCode.name()));
            }

            if (coordinator.isBlank() && areaCode.name().startsWith("CENTRO_")) {
                coordinator = safe(settings.getZoneCoordinatorEmails().get("CENTRO"));
            }
        }

        String settingsCc = joinEmailLists(humanTalent, payroll, coordinator);

        if (!settingsCc.isBlank()) {
            return settingsCc;
        }

        return normalizeEmailList(fallbackDefaultWorkerCc);
    }

    private SystemSettings getOrCreateSettings() {
        return repository.findById(SystemSettings.GLOBAL_ID).orElseGet(() -> {
            SystemSettings settings = new SystemSettings();
            settings.setId(SystemSettings.GLOBAL_ID);
            settings.setAutoSendApproverEmail(true);
            settings.setFrontendBaseUrl(safe(fallbackFrontendBaseUrl));
            settings.setUserAuditRetentionMonths(12);
            settings.setApproverEmails(normalizeEmailList(fallbackApproverEmails));
            settings.setApproverCc(normalizeEmailList(fallbackApproverCc));
            settings.setZoneCoordinatorEmails(defaultZoneMap());
            settings.setCreatedAt(LocalDateTime.now());
            settings.setUpdatedAt(LocalDateTime.now());

            return repository.save(settings);
        });
    }

    private Map<String, String> defaultZoneMap() {
        Map<String, String> zones = new LinkedHashMap<>();

        for (AreaCode areaCode : AreaCode.values()) {
            if (areaCode == AreaCode.EDIFICIO) {
                continue;
            }

            zones.put(areaCode.name(), "");
        }

        return zones;
    }

    private Map<String, String> normalizeZoneCoordinators(Map<String, String> input) {
        Map<String, String> normalized = defaultZoneMap();

        if (input == null) {
            return normalized;
        }

        input.forEach((key, value) -> {
            if (key == null) {
                return;
            }

            String normalizedKey = canonicalAreaKey(key);

            if (normalized.containsKey(normalizedKey)) {
                normalized.put(normalizedKey, normalizeEmailList(value));
            }
        });

        return normalized;
    }

    private int normalizeAuditRetentionMonths(Integer value) {
        if (value == null) {
            return 12;
        }

        if (value == 0) {
            return 0;
        }

        if (value < 3 || value > 60) {
            throw new IllegalArgumentException("La retencion de auditoria debe ser 24 horas de prueba o estar entre 3 y 60 meses.");
        }

        return value;
    }

    private String normalizeText(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }

    private String canonicalAreaKey(String value) {
        String normalized = safe(value)
                .toUpperCase()
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U')
                .replace('Ñ', 'N')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if ("PUERTO".equals(normalized) || "PUERTO_BOYACA".equals(normalized)) {
            return AreaCode.PUERTO_BOYACA.name();
        }
        if ("DIRECCION_DE_MANTENIMIENTO".equals(normalized)) {
            return AreaCode.DIRECCION_MANTENIMIENTO.name();
        }
        if ("DIRECCION_DE_OPERACION".equals(normalized)) {
            return AreaCode.DIRECCION_OPERACION.name();
        }
        if ("DIRECCION_DE_PERDIDAS".equals(normalized)) {
            return AreaCode.DIRECCION_PERDIDAS.name();
        }
        if ("VILLA_DE_LEYVA".equals(normalized) || "CENTRO_VILLA_DE_LEYVA".equals(normalized)) {
            return AreaCode.CENTRO_VILLA_DE_LEYVA.name();
        }
        if ("SAMACA".equals(normalized) || "CENTRO_SAMACA".equals(normalized)) {
            return AreaCode.CENTRO_SAMACA.name();
        }

        return normalized;
    }

    private String normalizeEmailList(String value) {
        String raw = safe(value);

        if (raw.isBlank()) {
            return "";
        }

        return Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String joinEmailLists(String... values) {
        return Arrays.stream(values)
                .map(this::normalizeEmailList)
                .filter(item -> !item.isBlank())
                .collect(Collectors.joining(","));
    }

    private boolean isOutdatedFrontendUrl(String value) {
        String normalized = safe(value).toLowerCase();
        return normalized.contains("localhost") ||
                normalized.contains("127.0.0.1") ||
                normalized.contains("0.0.0.0") ||
                normalized.contains("ssralturas.ebsa.com.co");
    }

    private String safe(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }
}
