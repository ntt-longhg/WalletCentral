package com.gateway.walletcentral.modules.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin dashboard counters computed with COUNT queries (no pagination)")
public class DashboardSummaryResponse {

    @Schema(description = "Total tenants")
    private long tenantCount;

    @Schema(description = "Total wallets")
    private long walletCount;

    @Schema(description = "Total services")
    private long serviceCount;

    @Schema(description = "Total transactions")
    private long transactionCount;

    @Schema(description = "Pending wallet plan requests")
    private long pendingPlanCount;

    @Schema(description = "Pending refund requests")
    private long pendingRefundCount;
}
