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
            notificationEventHandler.handleNotification(payload, channel, deliveryTag);
        } catch (Exception e) {
            log.error("Notification listener error: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            log.info("========== NOTIFICATION LISTENER END ==========");
            MDC.clear();
        }
    }
}
