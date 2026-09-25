package com.gateway.walletcentral.modules.transaction.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.transaction.handler.TransactionEventHandler;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    private final SystemConfigService configService;

    public TransactionListener(TransactionEventHandler transactionEventHandler,
            SystemConfigService configService) {
        this.transactionEventHandler = transactionEventHandler;
        this.configService = configService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRANSACTION, executor = "virtualThreadExecutor")
    public void handleTransactionEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered,
            Channel channel) throws IOException {

        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        log.info("========== TRANSACTION LISTENER START ==========");
        String event = (String) message.get("event");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }
        log.info("Transaction listener event: {} | payload: {} | thread: {}", event, payload,
                Thread.currentThread());

        try {
            switch (event) {
                case "BILLING_WEBHOOK":
                    transactionEventHandler.handleBillingCharge(message);
                    break;
                case "WALLET_PLAN":
                    transactionEventHandler.handleWalletPlanDeposit(message);
                    break;
                case "REFUND":
                    transactionEventHandler.handleRefundDeposit(message);
                    break;
                case "TRANSACTION_CREATED":
                    // Record was already persisted synchronously by TransactionService;
                    // no-op, ack to avoid poison message.
                    log.info("Transaction created event received, already persisted - acking");
                    break;
                default:
                    log.error("Unknown transaction event: {}", event);
                    break;
            }
            // Handler returned => its transaction committed => safe to ack
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // First failure => requeue for one retry (if mq.retry_enabled); redelivered failure => DLQ
            boolean requeue = configService.getBoolean("mq.retry_enabled", true)
                    && !Boolean.TRUE.equals(redelivered);
            log.error("Transaction listener error: redelivered={} requeue={}", redelivered, requeue, e);
            channel.basicNack(deliveryTag, false, requeue);
        } finally {
            log.info("========== TRANSACTION LISTENER END ==========");
            MDC.clear();
        }
    }
}
