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

        Map<String, Object> txnEvent = new HashMap<>();
        txnEvent.put("transactionId", event.getTransactionId());
        txnEvent.put("walletId", event.getWalletId());
        txnEvent.put("type", event.getType());
        txnEvent.put("amount", event.getAmount());
        txnEvent.put("balanceAfter", event.getBalanceAfter());
        messageProducer.publishTransaction(txnEvent);
    }
}
