package com.gateway.walletcentral.modules.walletplan.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.walletplan.handler.WalletPlanEventHandler;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
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
public class WalletPlanListener {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanListener.class);

    private final WalletPlanEventHandler walletPlanEventHandler;
    private final SystemConfigService configService;

    public WalletPlanListener(WalletPlanEventHandler walletPlanEventHandler,
            SystemConfigService configService) {
        this.walletPlanEventHandler = walletPlanEventHandler;
        this.configService = configService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WALLET_PLAN, executor = "virtualThreadExecutor")
    public void handleWalletPlanEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered,
            Channel channel) throws IOException {

        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        log.info("========== WALLET PLAN LISTENER START ==========");
        String event = (String) message.get("event");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");

        try {
            log.info("Wallet plan listener event: {} | payload: {} | thread: {}", event, payload,
                    Thread.currentThread());
            WalletPlanStatus status;
            try {
                status = WalletPlanStatus.valueOf(event);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.warn("Wallet plan listener unknown or null event: {} | payload: {}", event, payload);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            switch (status) {
                case APPROVED:
                    walletPlanEventHandler.handlerApproved(payload);
                    break;
                case REJECTED:
                    walletPlanEventHandler.handlerRejected(payload);
                    break;
                case PENDING:
                    log.info("Wallet plan PENDING event - nothing to do, acking");
                    break;
            }
            // Handler returned => its transaction committed => safe to ack
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // First failure => requeue for one retry (if mq.retry_enabled); redelivered failure => DLQ
            boolean requeue = configService.getBoolean("mq.retry_enabled", true)
                    && !Boolean.TRUE.equals(redelivered);
            log.error("Wallet plan listener error processing event: {} redelivered={} requeue={}",
                    event, redelivered, requeue, e);
            channel.basicNack(deliveryTag, false, requeue);
        } finally {
            log.info("========== WALLET PLAN LISTENER END ==========");
            MDC.clear();
        }
    }
}
