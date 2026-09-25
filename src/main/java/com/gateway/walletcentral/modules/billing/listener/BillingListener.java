package com.gateway.walletcentral.modules.billing.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.billing.handler.BillingEventHandler;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
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
public class BillingListener {

    private static final Logger log = LoggerFactory.getLogger(BillingListener.class);

    private final BillingEventHandler billingEventHandler;
    private final SystemConfigService configService;

    public BillingListener(BillingEventHandler billingEventHandler,
            SystemConfigService configService) {
        this.billingEventHandler = billingEventHandler;
        this.configService = configService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BILLING, executor = "virtualThreadExecutor")
    public void handleBillingEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered,
            Channel channel) throws IOException {

        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        log.info("========== BILLING LISTENER START ==========");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }

        try {
            log.info("Billing listener payload: {} | thread: {}", payload, Thread.currentThread());
            billingEventHandler.handleBillingCallback(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // First failure => requeue for one retry (if mq.retry_enabled); redelivered failure => DLQ
            boolean requeue = configService.getBoolean("mq.retry_enabled", true)
                    && !Boolean.TRUE.equals(redelivered);
            log.error("Billing listener error: redelivered={} requeue={}", redelivered, requeue, e);
            channel.basicNack(deliveryTag, false, requeue);
        } finally {
            log.info("========== BILLING LISTENER END ==========");
            MDC.clear();
        }
    }
}
