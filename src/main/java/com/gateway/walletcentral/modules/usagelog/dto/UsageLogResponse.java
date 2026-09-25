package com.gateway.walletcentral.modules.usagelog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Usage log response")
public class UsageLogResponse {

    @Schema(description = "Usage log ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Tenant name")
    private String tenantName;

    @Schema(description = "Service ID")
    private UUID serviceId;

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Wallet type snapshot", example = "PREPAID")
    private String walletTypeSnapshot;

    @Schema(description = "Total usage in units")
    private Integer totalUsage;

    @Schema(description = "Total charged amount")
    private BigDecimal totalCharged;

    @Schema(description = "Credit limit at time of usage")
    private BigDecimal creditLimitSnapshot;

    @Schema(description = "Available balance at time of usage")
    private BigDecimal availableBalanceSnapshot;

    @Schema(description = "Fee breakdown details")
    private Map<String, Object> feeBreakdown;

    @Schema(description = "Reference source system")
    private String referenceFrom;

    @Schema(description = "Reference ID from source system")
    private String referenceId;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
