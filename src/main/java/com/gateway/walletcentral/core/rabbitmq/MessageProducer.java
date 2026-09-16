package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTransaction(Map<String, Object> payload) {
        log.info("Publishing transaction event: {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_TRANSACTION,
                RabbitMQConfig.RK_TRANSACTION_BINDING,
                payload);
    }

    public void publishWalletPlan(Map<String, Object> payload) {
        log.info("Publishing wallet plan event: {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_WALLET,
                RabbitMQConfig.RK_WALLET_PLAN_BINDING,
                payload);
    }

    public void publishUsageLog(Map<String, Object> payload) {
        log.info("Publishing usage log event: {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_USAGE,
                RabbitMQConfig.RK_USAGE_BINDING,
                payload);
    }

    public void publishBilling(Map<String, Object> payload) {
        log.info("Publishing billing event: {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_BILLING,
                RabbitMQConfig.RK_BILLING_BINDING,
                payload);
    }

    public void publishNotification(Map<String, Object> payload) {
        log.info("Publishing notification event: {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NOTIFICATION,
                RabbitMQConfig.RK_NOTIFICATION_BINDING,
                payload);
    }
}
