package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.event.BillingProcessedEvent;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.billing.dto.BillingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class BillingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BillingEventPublisher.class);

    private final MessageProducer messageProducer;

    public BillingEventPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBillingProcessed(BillingProcessedEvent event) {
        log.info("Billing processed event received after commit: refId={}", event.getReferenceId());

        BillingEvent billingEvent = BillingEvent.builder()
                .tenantId(event.getTenantId())
                .serviceId(event.getServiceId())
                .serviceCode(event.getServiceCode())
                .serviceName(event.getServiceName())
                .usageUnits(event.getUsageUnits())
                .totalFee(event.getTotalFee())
                .walletId(event.getWalletId())
                .walletType(event.getWalletType())
                .balanceBefore(event.getBalanceBefore())
                .balanceAfter(event.getBalanceAfter())
                .creditLimit(event.getCreditLimit())
                .availableBalanceAfter(event.getAvailableBalanceAfter())
                .feeBreakdown(event.getFeeBreakdown())
                .referenceFrom(event.getReferenceFrom())
                .referenceId(event.getReferenceId())
                .createdAt(event.getCreatedAt())
                .description(event.getDescription())
                .webhookUrl(event.getWebhookUrl())
                .webhookAuth(event.getWebhookAuth())
                .build();

        Map<String, Object> txnEvent = new HashMap<>();
        txnEvent.put("event", "BILLING_WEBHOOK");
        txnEvent.put("payload", billingEvent);
        messageProducer.publishTransaction(txnEvent);
        log.info("Published BILLING_WEBHOOK transaction event after commit: refId={}", event.getReferenceId());

        messageProducer.publishUsageLog(txnEvent);
        log.info("Published BILLING_WEBHOOK usage event after commit: refId={}", event.getReferenceId());

        Map<String, Object> webhookEvent = new HashMap<>();
        webhookEvent.put("transactionId", event.getReferenceId());
        webhookEvent.put("usageLogId", event.getReferenceId());
        webhookEvent.put("webhookUrl", event.getWebhookUrl());
        webhookEvent.put("webhookAuth", event.getWebhookAuth());
        webhookEvent.put("response", billingEvent);
        messageProducer.publishBilling(webhookEvent);
        log.info("Published billing callback event after commit: refId={}", event.getReferenceId());
    }
}
