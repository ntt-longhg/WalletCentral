package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.event.RefundApprovedEvent;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Forwards approved refunds to the transaction queue so a REFUND
 * transaction record is created. Listens AFTER_COMMIT so the RabbitMQ
 * message is only published when the DB transaction has committed
 * successfully.
 */
@Component
public class RefundEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RefundEventPublisher.class);

    private final MessageProducer messageProducer;

    public RefundEventPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRefundApproved(RefundApprovedEvent event) {
        log.info("Refund approved event received after commit: refundRequestId={}", event.getRefundRequestId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("refundRequestId", event.getRefundRequestId());
        payload.put("originalTransactionId", event.getTransactionId());
        payload.put("walletId", event.getWalletId());
        payload.put("tenantId", event.getTenantId());
        payload.put("amount", event.getAmount());
        payload.put("balanceBefore", event.getBalanceBefore());
        payload.put("balanceAfter", event.getBalanceAfter());
        payload.put("availableBalanceBefore", event.getAvailableBalanceBefore());
        payload.put("availableBalanceAfter", event.getAvailableBalanceAfter());
        payload.put("description", "Hoàn tiền cho giao dịch " + event.getTransactionId());
        payload.put("reviewedBy", event.getReviewedBy());
        payload.put("referenceFrom", "REFUND");
        payload.put("referenceId", event.getRefundRequestId());

        Map<String, Object> txnEvent = new HashMap<>();
        txnEvent.put("event", "REFUND");
        txnEvent.put("payload", payload);
        messageProducer.publishTransaction(txnEvent);
        log.info("Published REFUND transaction event after commit: refundRequestId={}", event.getRefundRequestId());
    }
}
