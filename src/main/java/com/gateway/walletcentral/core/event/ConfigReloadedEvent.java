package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Published after the local config cache is reloaded from DB,
 * so dynamic components (schedulers, etc.) can re-read their settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigReloadedEvent {

    private Set<String> changedKeys;
}
