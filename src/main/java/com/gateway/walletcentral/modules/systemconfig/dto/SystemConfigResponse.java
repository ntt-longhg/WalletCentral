package com.gateway.walletcentral.modules.systemconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponse {

    @Schema(description = "Configuration ID")
    private String id;

    @Schema(description = "Configuration key", example = "smtp.host")
    private String key;

    @Schema(description = "Configuration value", example = "smtp.gmail.com")
    private String value;

    @Schema(description = "Configuration group", example = "SMTP")
    private String group;

    @Schema(description = "Configuration description")
    private String description;

    @Schema(description = "UI field type: text, number, password, textarea, select, radio, boolean, time", example = "text")
    private String fieldType;

    @Schema(description = "JSON options for select/radio fields", example = "[{\"value\":\"auto\",\"label\":\"Auto\"}]")
    private String fieldOptions;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
