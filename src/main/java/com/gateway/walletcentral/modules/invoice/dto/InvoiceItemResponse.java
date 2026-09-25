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
@Schema(description = "One billable line of an invoice, linked to its transaction")
public class InvoiceItemResponse {

    @Schema(description = "Usage log ID")
    private UUID usageLogId;

    @Schema(description = "Service name")
    private String serviceName;

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Usage units consumed")
    private Integer totalUsage;

    @Schema(description = "Fee charged for this usage")
    private BigDecimal totalCharged;

    @Schema(description = "Billing reference ID (links usage log and transaction)")
    private String referenceId;

    @Schema(description = "Whether this line was refunded (excluded from total)")
    private boolean refunded;

    @Schema(description = "Linked transaction ID, if any")
    private UUID transactionId;

    @Schema(description = "Linked transaction status, if any")
    private String transactionStatus;

    @Schema(description = "Usage timestamp")
    private LocalDateTime createdAt;
}
