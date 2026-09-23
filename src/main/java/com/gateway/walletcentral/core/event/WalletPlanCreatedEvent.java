package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPlanCreatedEvent {

    private String tenantId;
    private String walletPlanId;
    private String pricingPlanName;
    private String createdBy;
}
