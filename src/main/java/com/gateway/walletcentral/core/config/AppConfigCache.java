package com.gateway.walletcentral.core.config;

import com.gateway.walletcentral.modules.systemconfig.model.SystemConfig;
import com.gateway.walletcentral.modules.systemconfig.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory copy of system_configs.
 * <p>
 * - Loaded once at startup; every runtime read is served from this local map
 * (no DB pressure from hot paths like MQ listeners).
 * - UI edits only write to DB. They take effect after an explicit reload
 * (POST /api/v1/system-configs/reload), which diffs DB vs local, swaps the
 * map, and logs exactly what changed.
 * - Secret values (password/secret/...) are masked in logs.
 */
@Component
public class AppConfigCache {

    private static final Logger log = LoggerFactory.getLogger(AppConfigCache.class);

    private static final List<String> SECRET_SEGMENTS = List.of(
            "password", "passwd", "secret", "secrets", "credential", "credentials");

    private final SystemConfigRepository configRepository;
    private final ConcurrentHashMap<String, String> local = new ConcurrentHashMap<>();
    private volatile LocalDateTime loadedAt;

    public AppConfigCache(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @PostConstruct
    public void init() {
        Map<String, String> all = loadFromDb();
        local.putAll(all);
        loadedAt = LocalDateTime.now();
        log.info("========== APP CONFIG LOADED ({} entries) ==========", all.size());
        new TreeMap<>(all).forEach((key, value) ->
                log.info("config {} = {}", key, displayValue(key, value)));
        log.info("========== END APP CONFIG ==========");
    }

    /**
     * Reload from DB, swap the local map, and report the diff.
     * Thread-safe: readers always see either the full old or full new map.
     */
    public synchronized ConfigReloadResult reload() {
        Map<String, String> fresh = loadFromDb();
        Map<String, String> previous = new HashMap<>(local);

        List<ConfigChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> entry : fresh.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            if (!previous.containsKey(key)) {
                changes.add(new ConfigChange(key, null, newValue, ConfigChange.ChangeType.ADDED));
            } else if (!previous.get(key).equals(newValue)) {
                changes.add(new ConfigChange(key, previous.get(key), newValue, ConfigChange.ChangeType.CHANGED));
            }
        }
        for (String key : previous.keySet()) {
            if (!fresh.containsKey(key)) {
                changes.add(new ConfigChange(key, previous.get(key), null, ConfigChange.ChangeType.REMOVED));
            }
        }

        local.clear();
        local.putAll(fresh);
        loadedAt = LocalDateTime.now();

        int changed = 0;
        int added = 0;
        int removed = 0;
        for (ConfigChange change : changes) {
            switch (change.type()) {
                case CHANGED -> changed++;
                case ADDED -> added++;
                case REMOVED -> removed++;
            }
        }
        ConfigReloadResult result = new ConfigReloadResult(
                changes.stream().sorted((a, b) -> a.key().compareTo(b.key())).toList(),
                changed, added, removed, fresh.size());

        log.info("========== APP CONFIG RELOADED: {} changed, {} added, {} removed ({} total) ==========",
                changed, added, removed, fresh.size());
        for (ConfigChange change : result.changes()) {
            log.info("config {}: {}: '{}' -> '{}'",
                    change.key(), change.type(),
                    displayValue(change.key(), change.oldValue()),
                    displayValue(change.key(), change.newValue()));
        }
        if (result.totalChanged() == 0) {
            log.info("config: no changes detected, local cache is already up to date");
        }
        log.info("========== END APP CONFIG RELOAD ==========");
        return result;
    }

    public Map<String, String> snapshot() {
        return new HashMap<>(local);
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public String getValue(String key, String defaultValue) {
        String value = local.get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(getValue(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid long config '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        try {
            return new BigDecimal(getValue(key, defaultValue.toString()).trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid decimal config '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key, String.valueOf(defaultValue)).trim().toLowerCase();
        return switch (value) {
            case "true", "1", "yes", "on", "open", "enabled" -> true;
            case "false", "0", "no", "off", "closed", "disabled" -> false;
            default -> {
                log.warn("Invalid boolean config '{}', using default {}", key, defaultValue);
                yield defaultValue;
            }
        };
    }

    /**
     * A key is secret only when its LAST segment is a credential word
     * (smtp.password, client_secret) or a credential hash (*.password_hash).
     * This keeps harmless keys like auth.password_min_length or
     * auth.password_setup visible while masking real secrets.
     */
    public static boolean isSecretKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        if (lower.contains("private_key") || lower.contains("privatekey")) {
            return true;
        }
        String[] segments = lower.split("[._\\-]+");
        if (segments.length == 0) {
            return false;
        }
        String last = segments[segments.length - 1];
        if (SECRET_SEGMENTS.contains(last)) {
            return true;
        }
        return (last.contains("password") || last.contains("secret") || last.contains("credential"))
                && last.contains("hash");
    }

    public static String displayValue(String key, String value) {
        if (value == null) {
            return "<removed>";
        }
        return isSecretKey(key) ? "********" : value;
    }

    private Map<String, String> loadFromDb() {
        Map<String, String> all = new HashMap<>();
        for (SystemConfig config : configRepository.findAll()) {
            if (config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
                all.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return all;
    }
}
