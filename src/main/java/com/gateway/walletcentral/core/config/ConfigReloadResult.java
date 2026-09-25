package com.gateway.walletcentral.core.config;

import java.util.List;

/**
 * Result of a config reload: what changed in the local cache vs DB.
 */
public record ConfigReloadResult(
        List<ConfigChange> changes,
        int changedCount,
        int addedCount,
        int removedCount,
        int totalCount) {

    public int totalChanged() {
        return changedCount + addedCount + removedCount;
    }
}
