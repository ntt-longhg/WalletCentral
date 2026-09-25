package com.gateway.walletcentral.core.config;

/**
 * One config entry change detected during reload.
 */
public record ConfigChange(
        String key,
        String oldValue,
        String newValue,
        ChangeType type) {

    public enum ChangeType {
        CHANGED,
        ADDED,
        REMOVED
    }
}
