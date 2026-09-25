package com.gateway.walletcentral.modules.servicecatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service response")
public class ServiceResponse {

    @Schema(description = "Service ID")
    private UUID id;

    @Schema(description = "Service code", example = "SMS")
    private String code;

    @Schema(description = "Service name", example = "SMS Service")
    private String name;

    @Schema(description = "Service description")
    private String description;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
