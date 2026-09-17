package com.gateway.walletcentral.modules.notification.handler;

import com.gateway.walletcentral.modules.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationService notificationService;

    public NotificationEventHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void handleNotification(Map<String, Object> payload, Channel channel, long deliveryTag) throws IOException {
        String tenantId = (String) payload.get("tenantId");
        String type = (String) payload.get("type");
        String title = (String) payload.get("title");
        String message = (String) payload.get("message");
        String referenceType = (String) payload.get("referenceType");
        String referenceId = (String) payload.get("referenceId");
        log.info("Notification handler - tenantId: {} | type: {} | title: {} | thread: {}", tenantId, type, title,
                Thread.currentThread());

        try {
            notificationService.saveAndPush(
                    UUID.fromString(tenantId),
                    type,
                    title,
                    message,
                    referenceType,
                    referenceId);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Notification handler error: tenantId={}", tenantId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
