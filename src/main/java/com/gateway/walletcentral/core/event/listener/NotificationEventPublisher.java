package com.gateway.walletcentral.core.event.listener;

import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final MessageProducer messageProducer;

    public NotificationEventPublisher(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotification(NotificationEvent event) {
        log.info("Notification event received after commit: type={} tenantId={}", event.getType(), event.getTenantId());

        Map<String, Object> notificationEvent = new HashMap<>();
        notificationEvent.put("tenantId", event.getTenantId());
        notificationEvent.put("type", event.getType());
        notificationEvent.put("title", event.getTitle());
        notificationEvent.put("message", event.getMessage());
        notificationEvent.put("referenceType", event.getReferenceType());
        notificationEvent.put("referenceId", event.getReferenceId());
        messageProducer.publishNotification(notificationEvent);
    }
}
