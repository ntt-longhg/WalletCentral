package com.gateway.walletcentral.modules.notification.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.notification.handler.NotificationEventHandler;
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
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationEventHandler notificationEventHandler;

    public NotificationListener(NotificationEventHandler notificationEventHandler) {
        this.notificationEventHandler = notificationEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION, executor = "virtualThreadExecutor")
    public void handleNotificationEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered,
            Channel channel) throws IOException {

        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        log.info("========== NOTIFICATION LISTENER START ==========");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }

        try {
            log.info("Notification listener payload: {} | thread: {}", payload, Thread.currentThread());
            notificationEventHandler.handleNotification(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // First failure => requeue for one retry; redelivered failure => DLQ
            boolean requeue = !Boolean.TRUE.equals(redelivered);
            log.error("Notification listener error: redelivered={} requeue={}", redelivered, requeue, e);
            channel.basicNack(deliveryTag, false, requeue);
        } finally {
            log.info("========== NOTIFICATION LISTENER END ==========");
            MDC.clear();
        }
    }
}
