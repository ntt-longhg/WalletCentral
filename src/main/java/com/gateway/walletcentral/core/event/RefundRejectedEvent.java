package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRejectedEvent {

    private String refundRequestId;
    private String tenantId;
    private String reviewedBy;
    private String rejectReason;
}
