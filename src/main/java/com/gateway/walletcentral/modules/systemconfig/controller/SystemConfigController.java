package com.gateway.walletcentral.modules.systemconfig.controller;

import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.ConfigReloadResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigResponse;
import com.gateway.walletcentral.modules.systemconfig.dto.SystemConfigUpdateRequest;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system-configs")
@Tag(name = "System Config", description = "System configuration management operations")
public class SystemConfigController {

    private final SystemConfigService configService;

    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "List all system configurations, optionally filtered by group")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> list(
            @RequestParam(required = false) String group) {
        List<SystemConfigResponse> configs = configService.getAll(group);
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @PutMapping
    @Operation(summary = "Bulk save system configurations to DB (takes effect after reload)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestBody Map<String, List<SystemConfigUpdateRequest>> body) {
        List<SystemConfigUpdateRequest> configs = body.get("configs");
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("Configs list is required");
        }
        int saved = configService.updateConfigs(configs);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.<String, Object>of("savedCount", saved),
                "Saved " + saved + " configuration(s) to database. Press Sync to apply."));
    }

    @PostMapping("/reload")
    @Operation(summary = "Reload local config cache from DB and apply immediately (returns the diff)")
    public ResponseEntity<ApiResponse<ConfigReloadResponse>> reload() {
        ConfigReloadResponse result = configService.reloadConfigs();
        String message = result.getTotalChanged() == 0
                ? "Local config is already up to date."
                : "Applied " + result.getTotalChanged() + " change(s) (" + result.getChangedCount()
                        + " changed, " + result.getAddedCount() + " added, " + result.getRemovedCount() + " removed).";
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }
}
