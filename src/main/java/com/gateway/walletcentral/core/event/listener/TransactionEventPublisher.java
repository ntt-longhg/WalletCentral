package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.event.TransactionCreatedEvent;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final MessageProducer messageProducer;

    public TransactionEventPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Transaction created event received after commit: transactionId={} type={}",
                event.getTransactionId(), event.getType());

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", event.getTransactionId());
        payload.put("walletId", event.getWalletId());
        payload.put("type", event.getType());
        payload.put("amount", event.getAmount());
        payload.put("balanceAfter", event.getBalanceAfter());
        payload.put("metadata", event.getMetadata());

        Map<String, Object> txnEvent = new HashMap<>();
        txnEvent.put("event", "TRANSACTION_CREATED");
        txnEvent.put("payload", payload);
        messageProducer.publishTransaction(txnEvent);
    }
}
