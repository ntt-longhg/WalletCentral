package com.gateway.walletcentral.modules.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWebhookResponse {

    @Schema(description = "Transaction ID for tracking")
    private String transactionId;

    @Schema(description = "Tenant ID")
    private String tenantId;

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Service name")
    private String serviceName;

    @Schema(description = "Usage units billed")
    private Integer usageUnits;

    @Schema(description = "Total fee charged")
    private BigDecimal totalCharged;

    @Schema(description = "Fee breakdown details")
    private FeeBreakdownDto feeBreakdown;

    @Schema(description = "Wallet balance after transaction")
    private BigDecimal balanceAfter;

    @Schema(description = "Available balance after transaction")
    private BigDecimal availableBalanceAfter;

    @Schema(description = "Transaction status", example = "SUCCESS")
    private String status;

    @Schema(description = "Transaction creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Reference ID from request")
    private String referenceId;

    @Schema(description = "Metadata from request")
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeBreakdownDto {
        private String strategy;
        private BigDecimal initialFee;
        private Integer initialUnits;
        private BigDecimal subsequentFee;
        private Integer subsequentUnits;
        private BigDecimal totalFee;
    }
}
