package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.event.WalletPlanApprovedEvent;
import com.gateway.walletcentral.core.event.WalletPlanRejectedEvent;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Forwards wallet-plan status changes to the wallet_plan queue.
 * Listens AFTER_COMMIT so the RabbitMQ message is only published
 * when the DB transaction has committed successfully.
 */
@Component
public class WalletPlanStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanStatusPublisher.class);

    private final MessageProducer messageProducer;

    public WalletPlanStatusPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletPlanApproved(WalletPlanApprovedEvent event) {
        log.info("Wallet plan approved event received after commit: walletPlanId={}", event.getWalletPlanId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", event.getWalletPlanId());
        payload.put("tenantId", event.getTenantId());

        Map<String, Object> message = new HashMap<>();
        message.put("event", WalletPlanStatus.APPROVED.name());
        message.put("payload", payload);
        messageProducer.publishWalletPlan(message);
        log.info("Published wallet_plan APPROVED event after commit: walletPlanId={}", event.getWalletPlanId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletPlanRejected(WalletPlanRejectedEvent event) {
        log.info("Wallet plan rejected event received after commit: walletPlanId={}", event.getWalletPlanId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", event.getWalletPlanId());
        payload.put("tenantId", event.getTenantId());

        Map<String, Object> message = new HashMap<>();
        message.put("event", WalletPlanStatus.REJECTED.name());
        message.put("payload", payload);
        messageProducer.publishWalletPlan(message);
        log.info("Published wallet_plan REJECTED event after commit: walletPlanId={}", event.getWalletPlanId());
    }
}
