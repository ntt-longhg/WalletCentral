package com.gateway.walletcentral.modules.refund.dto;

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
@Schema(description = "Refund request response")
public class RefundResponse {

    @Schema(description = "Refund request ID")
    private UUID id;

    @Schema(description = "Original transaction ID")
    private UUID transactionId;

    @Schema(description = "Original transaction amount")
    private BigDecimal transactionAmount;

    @Schema(description = "Original transaction type")
    private String transactionType;

    @Schema(description = "Original transaction status")
    private String transactionStatus;

    @Schema(description = "Wallet ID")
    private UUID walletId;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Tenant name")
    private String tenantName;

    @Schema(description = "Refund amount")
    private BigDecimal amount;

    @Schema(description = "Refund status", example = "PENDING")
    private String status;

    @Schema(description = "Reason for refund request")
    private String reason;

    @Schema(description = "Reason for rejection")
    private String rejectReason;

    @Schema(description = "User who requested the refund")
    private String requestedBy;

    @Schema(description = "Admin who reviewed the refund")
    private String reviewedBy;

    @Schema(description = "Timestamp when refund was reviewed")
    private LocalDateTime reviewedAt;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
}
