package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundApprovedEvent {

    private String refundRequestId;
    private String transactionId;
    private String walletId;
    private String tenantId;
    private BigDecimal amount;
    private String reviewedBy;
}
