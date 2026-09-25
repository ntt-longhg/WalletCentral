package com.gateway.walletcentral.modules.transaction.dto;

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
@Schema(description = "Transaction response")
public class TransactionResponse {

    @Schema(description = "Transaction ID")
    private UUID id;

    @Schema(description = "Wallet ID")
    private UUID walletId;

    @Schema(description = "Transaction amount", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Transaction type", example = "CHARGE")
    private String type;

    @Schema(description = "Balance before transaction")
    private BigDecimal balanceBefore;

    @Schema(description = "Balance after transaction")
    private BigDecimal balanceAfter;

    @Schema(description = "Available balance before")
    private BigDecimal availableBalanceBefore;

    @Schema(description = "Available balance after")
    private BigDecimal availableBalanceAfter;

    @Schema(description = "Transaction status", example = "SUCCESS")
    private String status;

    @Schema(description = "Transaction description")
    private String description;

    @Schema(description = "Reference source system", example = "API")
    private String referenceFrom;

    @Schema(description = "Reference ID from source system", example = "TXN-12345")
    private String referenceId;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
