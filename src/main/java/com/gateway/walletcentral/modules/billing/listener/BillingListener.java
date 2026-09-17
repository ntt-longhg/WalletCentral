package com.gateway.walletcentral.modules.billing.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.billing.handler.BillingEventHandler;
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
public class BillingListener {

    private static final Logger log = LoggerFactory.getLogger(BillingListener.class);

    private final BillingEventHandler billingEventHandler;

    public BillingListener(BillingEventHandler billingEventHandler) {
        this.billingEventHandler = billingEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BILLING, executor = "virtualThreadExecutor")
    public void handleBillingEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {

        log.info("========== BILLING LISTENER START ==========");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }

        try {
            log.info("Billing listener payload: {} | thread: {}", payload, Thread.currentThread());
            billingEventHandler.handleBillingCallback(payload, channel, deliveryTag);
        } catch (Exception e) {
            log.error("Billing listener error: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            log.info("========== BILLING LISTENER END ==========");
        }
    }
}
