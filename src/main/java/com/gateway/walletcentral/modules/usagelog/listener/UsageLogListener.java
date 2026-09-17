package com.gateway.walletcentral.modules.usagelog.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.usagelog.handler.UsageLogEventHandler;
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
public class UsageLogListener {

    private static final Logger log = LoggerFactory.getLogger(UsageLogListener.class);

    private final UsageLogEventHandler usageLogEventHandler;

    public UsageLogListener(UsageLogEventHandler usageLogEventHandler) {
        this.usageLogEventHandler = usageLogEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USAGE, executor = "virtualThreadExecutor")
    public void handleUsageLogEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {

        log.info("========== USAGE LOG LISTENER START ==========");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            payload = message;
        }

        try {
            log.info("UsageLog listener payload: {} | thread: {}", payload, Thread.currentThread());
            usageLogEventHandler.handleUsageLogRecorded(payload, channel, deliveryTag);
        } catch (Exception e) {
            log.error("UsageLog listener error: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            log.info("========== USAGE LOG LISTENER END ==========");
        }
    }
}
