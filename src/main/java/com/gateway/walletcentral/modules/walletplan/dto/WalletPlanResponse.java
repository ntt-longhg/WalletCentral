package com.gateway.walletcentral.modules.walletplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wallet plan response")
public class WalletPlanResponse {

    @Schema(description = "Wallet plan ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Tenant name")
    private String tenantName;

    @Schema(description = "Pricing plan ID")
    private UUID pricingPlanId;

    @Schema(description = "Pricing plan name")
    private String pricingPlanName;

    @Schema(description = "Plan price")
    private BigDecimal price;

    @Schema(description = "Bonus amount calculated")
    private BigDecimal bonusAmount;

    @Schema(description = "Credited amount")
    private BigDecimal creditedAmount;

    @Schema(description = "Balance before")
    private BigDecimal balanceBefore;

    @Schema(description = "Balance after")
    private BigDecimal balanceAfter;

    @Schema(description = "Credit limit before")
    private BigDecimal creditLimitBefore;

    @Schema(description = "Credit limit after")
    private BigDecimal creditLimitAfter;

    @Schema(description = "Plan status", example = "PENDING")
    private String status;

    @Schema(description = "Approval timestamp")
    private LocalDateTime approvedAt;

    @Schema(description = "Approved by")
    private String approvedBy;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
