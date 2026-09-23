package com.gateway.walletcentral.core.event;

import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProcessedEvent {

    private String tenantId;
    private String serviceId;
    private String serviceCode;
    private String serviceName;
    private Integer usageUnits;
    private BigDecimal totalFee;
    private String walletId;
    private String walletType;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal creditLimit;
    private BigDecimal availableBalanceAfter;
    private FeeBreakdownStructure feeBreakdown;
    private String referenceFrom;
    private String referenceId;
    private OffsetDateTime createdAt;
    private String description;
    private String webhookUrl;
    private String webhookAuth;
    private Object metadata;
}
