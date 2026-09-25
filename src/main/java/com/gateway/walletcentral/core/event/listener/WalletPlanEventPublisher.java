package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.walletplan.dto.WalletPlanEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class WalletPlanEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanEventPublisher.class);

    private final MessageProducer messageProducer;

    public WalletPlanEventPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletPlanApproved(WalletPlanEvent event) {
        log.info("Wallet plan approved event after commit: walletPlanId={}", event.getWalletPlanId());

        WalletPlanEvent walletPlanEvent = WalletPlanEvent.builder()
                .walletPlanId(event.getWalletPlanId())
                .tenantId(event.getTenantId())
                .walletId(event.getWalletId())
                .amount(event.getAmount())
                .balanceBefore(event.getBalanceBefore())
                .balanceAfter(event.getBalanceAfter())
                .availableBalanceBefore(event.getAvailableBalanceBefore())
                .availableBalanceAfter(event.getAvailableBalanceAfter())
                .description(event.getDescription())
                .approvedBy(event.getApprovedBy())
                .referenceFrom("WALLET_PLAN")
                .referenceId(event.getWalletPlanId())
                .build();

        Map<String, Object> txnEvent = new HashMap<>();
        txnEvent.put("event", "WALLET_PLAN");
        txnEvent.put("payload", walletPlanEvent);
        messageProducer.publishTransaction(txnEvent);
        log.info("Published WALLET_PLAN transaction event: walletPlanId={}", event.getWalletPlanId());
    }
}
