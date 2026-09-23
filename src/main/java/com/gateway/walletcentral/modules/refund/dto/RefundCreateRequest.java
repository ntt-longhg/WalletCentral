package com.gateway.walletcentral.modules.refund.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a refund request")
public class RefundCreateRequest {

    @NotNull(message = "Transaction ID must not be null")
    @Schema(description = "Transaction ID to refund (must be a CHARGE transaction)")
    private UUID transactionId;

    @Schema(description = "Reason for refund request")
    private String reason;

    @Schema(description = "User who requested the refund", example = "tenant_user")
    private String requestedBy;
}
