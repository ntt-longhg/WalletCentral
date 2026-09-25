package com.gateway.walletcentral.modules.tenant.dto;

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
@Schema(description = "Tenant response")
public class TenantResponse {

    @Schema(description = "Tenant ID")
    private UUID id;

    @Schema(description = "Tenant name", example = "Acme Corp")
    private String name;

    @Schema(description = "Client ID", example = "acme-corp")
    private String clientId;

    @Schema(description = "Comma-separated allowed domains")
    private String allowedDomains;

    @Schema(description = "Tenant status", example = "ACTIVE")
    private String status;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
