package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPlanRejectedEvent {

    private String walletPlanId;
    private String tenantId;
    private String pricingPlanName;
    private String approvedBy;
    private String rejectReason;
}
