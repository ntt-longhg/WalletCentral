package com.gateway.walletcentral.modules.invoice.dto;

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
@Schema(description = "Invoice response")
public class InvoiceResponse {

    @Schema(description = "Invoice ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Tenant name")
    private String tenantName;

    @Schema(description = "Wallet ID")
    private UUID walletId;

    @Schema(description = "Billing period", example = "2024-01")
    private String billingPeriod;

    @Schema(description = "Total invoice amount")
    private BigDecimal totalAmount;

    @Schema(description = "Invoice status", example = "ISSUED")
    private String status;

    @Schema(description = "Note (e.g. zero-amount auto-payment reason)")
    private String note;

    @Schema(description = "Invoice due date")
    private LocalDateTime dueDate;

    @Schema(description = "Updated by")
    private String updatedBy;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
