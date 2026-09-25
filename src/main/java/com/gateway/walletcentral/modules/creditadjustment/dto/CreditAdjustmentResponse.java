package com.gateway.walletcentral.modules.creditadjustment.dto;

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
@Schema(description = "Credit adjustment response")
public class CreditAdjustmentResponse {

    @Schema(description = "Credit adjustment ID")
    private UUID id;

    @Schema(description = "Wallet ID")
    private UUID walletId;

    @Schema(description = "Credit limit before adjustment")
    private BigDecimal creditLimitBefore;

    @Schema(description = "Credit limit after adjustment")
    private BigDecimal creditLimitAfter;

    @Schema(description = "Adjustment amount")
    private BigDecimal adjustmentAmount;

    @Schema(description = "Adjustment type", example = "INCREASE")
    private String type;

    @Schema(description = "Adjustment reason")
    private String reason;

    @Schema(description = "Reference source system")
    private String referenceFrom;

    @Schema(description = "Reference ID from source system")
    private String referenceId;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
