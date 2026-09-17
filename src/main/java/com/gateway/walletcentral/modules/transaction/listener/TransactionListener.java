package com.gateway.walletcentral.modules.transaction.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.transaction.handler.TransactionEventHandler;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class TransactionListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionListener.class);

    private final TransactionEventHandler transactionEventHandler;

    public TransactionListener(TransactionEventHandler transactionEventHandler) {
        this.transactionEventHandler = transactionEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRANSACTION, executor = "virtualThreadExecutor")
    public void handleTransactionEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {

        log.info("========== TRANSACTION LISTENER START ==========");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }

        try {
            log.info("Transaction listener payload: {} | thread: {}", payload, Thread.currentThread());
            transactionEventHandler.handleTransactionCreated(payload, channel, deliveryTag);
        } catch (Exception e) {
            log.error("Transaction listener error: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            log.info("========== TRANSACTION LISTENER END ==========");
        }
    }
}
