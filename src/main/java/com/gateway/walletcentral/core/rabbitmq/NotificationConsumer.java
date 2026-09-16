package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION, executor = "virtualThreadExecutor")
    public void handleNotificationCreated(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        String tenantId = (String) message.get("tenantId");
        String type = (String) message.get("type");
        String title = (String) message.get("title");
        String msg = (String) message.get("message");
        String referenceType = (String) message.get("referenceType");
        String referenceId = (String) message.get("referenceId");

        log.info("========== NOTIFICATION CONSUMER START ==========");
        log.info("TenantId: {} | Type: {} | Title: {}", tenantId, type, title);

        try {
            notificationService.saveAndPush(
                    UUID.fromString(tenantId),
                    type,
                    title,
                    msg,
                    referenceType,
                    referenceId);

            log.info("========== NOTIFICATION CONSUMER END ========== SUCCESS tenant={}", tenantId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("========== NOTIFICATION CONSUMER END ========== FAILED tenant={}", tenantId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
