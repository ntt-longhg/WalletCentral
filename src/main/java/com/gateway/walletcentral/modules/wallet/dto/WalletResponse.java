package com.gateway.walletcentral.modules.wallet.dto;

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
@Schema(description = "Wallet response")
public class WalletResponse {

    @Schema(description = "Wallet ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Tenant name")
    private String tenantName;

    @Schema(description = "Wallet type", example = "PREPAID")
    private String type;

    @Schema(description = "Current balance", example = "5000.00")
    private BigDecimal balance;

    @Schema(description = "Credit limit", example = "10000.00")
    private BigDecimal creditLimit;

    @Schema(description = "Available balance (balance + credit limit)", example = "15000.00")
    private BigDecimal availableBalance;

    @Schema(description = "Wallet status", example = "ACTIVE")
    private String status;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
