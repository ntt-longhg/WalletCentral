package com.gateway.walletcentral.modules.walletplan.listener;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.walletplan.handler.WalletPlanEventHandler;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
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
public class WalletPlanListener {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanListener.class);

    private final WalletPlanEventHandler walletPlanEventHandler;

    public WalletPlanListener(WalletPlanEventHandler walletPlanEventHandler) {
        this.walletPlanEventHandler = walletPlanEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WALLET_PLAN, executor = "virtualThreadExecutor")
    public void handleWalletPlanEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {

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
                    walletPlanEventHandler.handlerApproved(payload, channel, deliveryTag);
                    break;
                case REJECTED:
                    walletPlanEventHandler.handlerRejected(payload, channel, deliveryTag);
                    break;
                case PENDING:
                    break;
            }
        } catch (Exception e) {
            log.error("Wallet plan listener error processing wallet plan event: {}", event, e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            log.info("========== WALLET PLAN LISTENER END ==========");
        }
    }
}
