package com.gateway.walletcentral.modules.systemconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of reloading the local config cache from DB")
public class ConfigReloadResponse {

    @Schema(description = "Number of keys whose value changed", example = "2")
    private int changedCount;

    @Schema(description = "Number of newly added keys", example = "1")
    private int addedCount;

    @Schema(description = "Number of removed keys", example = "0")
    private int removedCount;

    @Schema(description = "Total number of differences", example = "3")
    private int totalChanged;

    @Schema(description = "Total keys in local cache after reload", example = "42")
    private int totalCount;

    @Schema(description = "When the local cache was reloaded")
    private LocalDateTime loadedAt;

    @Schema(description = "Per-key differences, sorted by key")
    private List<ConfigChangeDto> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "One config difference")
    public static class ConfigChangeDto {

        @Schema(description = "Config key", example = "alert.low_balance_threshold")
        private String key;

        @Schema(description = "Value before reload (null when added)")
        private String oldValue;

        @Schema(description = "Value after reload (null when removed)")
        private String newValue;

        @Schema(description = "CHANGED | ADDED | REMOVED", example = "CHANGED")
        private String changeType;

        @Schema(description = "Whether the value is secret (UI should mask it)", example = "false")
        private boolean secret;
    }
}
