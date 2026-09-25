package com.gateway.walletcentral.modules.systemconfig.service;

import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigUpdateRequest;
import com.gateway.walletcentral.modules.systemconfig.model.SystemConfig;
import com.gateway.walletcentral.modules.systemconfig.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class SystemConfigService {

    private final SystemConfigRepository configRepository;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public SystemConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
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

    public void updateConfigs(List<SystemConfigUpdateRequest> updates) {
        for (SystemConfigUpdateRequest update : updates) {
            SystemConfig config = configRepository.findByConfigKey(update.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("SystemConfig", "key", update.getKey()));
            config.setConfigValue(update.getValue());
            config.setUpdatedAt(LocalDateTime.now());
            configRepository.save(config);
            cache.put(update.getKey(), update.getValue());
        }
    }

    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k ->
                configRepository.findByConfigKey(k)
                        .map(SystemConfig::getConfigValue)
                        .filter(v -> !v.isBlank())
                        .orElse(defaultValue)
        );
    }

    public void invalidateCache() {
        cache.clear();
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .id(config.getId().toString())
                .key(config.getConfigKey())
                .value(config.getConfigValue())
                .group(config.getConfigGroup())
                .description(config.getDescription())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
