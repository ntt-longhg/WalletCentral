package com.gateway.walletcentral.modules.systemconfig.service;

import com.gateway.walletcentral.core.config.AppConfigCache;
import com.gateway.walletcentral.core.config.ConfigReloadResult;
import com.gateway.walletcentral.core.event.ConfigReloadedEvent;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.systemconfig.dto.ConfigReloadResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigUpdateRequest;
import com.gateway.walletcentral.modules.systemconfig.model.SystemConfig;
import com.gateway.walletcentral.modules.systemconfig.repository.SystemConfigRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SystemConfigService {

    private final SystemConfigRepository configRepository;
    private final AppConfigCache configCache;
    private final ApplicationEventPublisher eventPublisher;

    public SystemConfigService(SystemConfigRepository configRepository,
            AppConfigCache configCache,
            ApplicationEventPublisher eventPublisher) {
        this.configRepository = configRepository;
        this.configCache = configCache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAll(String group) {
        List<SystemConfig> configs;
        if (group != null && !group.isBlank()) {
            configs = configRepository.findByConfigGroupOrderByConfigKey(group.toUpperCase());
        } else {
            configs = configRepository.findAllByOrderByConfigGroupAscConfigKeyAsc();
        }
        return configs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getGrouped() {
        return configRepository.findAllByOrderByConfigGroupAscConfigKeyAsc()
                .stream().map(this::toResponse).toList();
    }

    /**
     * Persist edits to DB only. They do NOT take effect until
     * {@link #reloadConfigs()} is called (explicit Sync action on UI).
     *
     * @return number of keys saved
     */
    public int updateConfigs(List<SystemConfigUpdateRequest> updates) {
        for (SystemConfigUpdateRequest update : updates) {
            SystemConfig config = configRepository.findByConfigKey(update.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", "key", update.getKey()));
            config.setConfigValue(update.getValue());
            config.setUpdatedAt(LocalDateTime.now());
            configRepository.save(config);
        }
        return updates.size();
    }

    /**
     * Reload the local cache from DB and notify dynamic components
     * (schedulers, etc.) via {@link ConfigReloadedEvent}.
     */
    public ConfigReloadResponse reloadConfigs() {
        ConfigReloadResult result = configCache.reload();
        eventPublisher.publishEvent(ConfigReloadedEvent.builder()
                .changedKeys(result.changes().stream()
                        .map(c -> c.key())
                        .collect(Collectors.toSet()))
                .build());
        return ConfigReloadResponse.builder()
                .changedCount(result.changedCount())
                .addedCount(result.addedCount())
                .removedCount(result.removedCount())
                .totalChanged(result.totalChanged())
                .totalCount(result.totalCount())
                .loadedAt(configCache.getLoadedAt())
                .changes(result.changes().stream()
                        .map(c -> ConfigReloadResponse.ConfigChangeDto.builder()
                                .key(c.key())
                                .oldValue(c.oldValue())
                                .newValue(c.newValue())
                                .changeType(c.type().name())
                                .secret(AppConfigCache.isSecretKey(c.key()))
                                .build())
                        .toList())
                .build();
    }

    // Runtime reads are served from the local cache (no DB pressure).
    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        return configCache.getValue(key, defaultValue);
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        return configCache.getInt(key, defaultValue);
    }

    @Transactional(readOnly = true)
    public long getLong(String key, long defaultValue) {
        return configCache.getLong(key, defaultValue);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return configCache.getBigDecimal(key, defaultValue);
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        return configCache.getBoolean(key, defaultValue);
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .id(config.getId().toString())
                .key(config.getConfigKey())
                .value(config.getConfigValue())
                .group(config.getConfigGroup())
                .description(config.getDescription())
                .fieldType(config.getFieldType())
                .fieldOptions(config.getFieldOptions())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
