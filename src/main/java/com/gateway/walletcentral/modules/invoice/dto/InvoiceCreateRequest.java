package com.gateway.walletcentral.modules.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
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
@Schema(description = "Request to create an invoice")
public class InvoiceCreateRequest {

    @NotNull(message = "Tenant ID must not be null")
    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @NotNull(message = "Wallet ID must not be null")
    @Schema(description = "Wallet ID")
    private UUID walletId;

    @NotBlank(message = "Billing period must not be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Billing period must be in yyyy-MM format")
    @Schema(description = "Billing period", example = "2024-01")
    private String billingPeriod;

    @NotNull(message = "Total amount must not be null")
    @PositiveOrZero(message = "Total amount must be zero or positive")
    @Schema(description = "Total invoice amount", example = "1500.00")
    private BigDecimal totalAmount;

    @NotNull(message = "Due date must not be null")
    @Schema(description = "Invoice due date")
    private LocalDateTime dueDate;

    @NotBlank(message = "Updated by must not be blank")
    @Schema(description = "Creator identifier", example = "admin")
    private String updatedBy;
}
