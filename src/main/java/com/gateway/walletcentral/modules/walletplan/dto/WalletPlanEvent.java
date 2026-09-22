package com.gateway.walletcentral.modules.walletplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPlanEvent {
    private String walletPlanId;
    private String tenantId;
    private String walletId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal availableBalanceBefore;
    private BigDecimal availableBalanceAfter;
    private String description;
    private String approvedBy;
    private String referenceFrom;
    private String referenceId;

}
