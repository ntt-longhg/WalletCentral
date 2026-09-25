package com.gateway.walletcentral.modules.usagelog.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.usagelog.handler.UsageLogEventHandler;
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
public class UsageLogListener {

    private static final Logger log = LoggerFactory.getLogger(UsageLogListener.class);

    private final UsageLogEventHandler usageLogEventHandler;
    private final SystemConfigService configService;

    public UsageLogListener(UsageLogEventHandler usageLogEventHandler,
            SystemConfigService configService) {
        this.usageLogEventHandler = usageLogEventHandler;
        this.configService = configService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USAGE, executor = "virtualThreadExecutor")
    public void handleUsageLogEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered,
            Channel channel) throws IOException {

        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        log.info("========== USAGE LOG LISTENER START ==========");
        String event = (String) message.get("event");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }
        log.info("UsageLog listener event: {} | payload: {} | thread: {}", event, payload,
                Thread.currentThread());

        try {
            switch (event) {
                case "BILLING_WEBHOOK":
                    usageLogEventHandler.handleBillingUsageLog(message);
                    break;
                default:
                    log.error("Unknown usage log event: {}", event);
                    break;
            }
            // Handler returned => its transaction committed => safe to ack
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // First failure => requeue for one retry (if mq.retry_enabled); redelivered failure => DLQ
            boolean requeue = configService.getBoolean("mq.retry_enabled", true)
                    && !Boolean.TRUE.equals(redelivered);
            log.error("UsageLog listener error: redelivered={} requeue={}", redelivered, requeue, e);
            channel.basicNack(deliveryTag, false, requeue);
        } finally {
            log.info("========== USAGE LOG LISTENER END ==========");
            MDC.clear();
        }
    }
}
