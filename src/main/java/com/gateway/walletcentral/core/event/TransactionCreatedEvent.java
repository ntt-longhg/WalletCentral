package com.gateway.walletcentral.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {

    private String transactionId;
    private String walletId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Map<String, Object> metadata;
}
