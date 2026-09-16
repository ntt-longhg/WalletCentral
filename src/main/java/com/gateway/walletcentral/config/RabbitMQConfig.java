package com.gateway.walletcentral.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_TRANSACTION = "walletcentral.transaction.exchange";
    public static final String EXCHANGE_WALLET = "walletcentral.wallet.exchange";
    public static final String EXCHANGE_WALLET_PLAN = "walletcentral.wallet.plan.exchange";
    public static final String EXCHANGE_USAGE = "walletcentral.usage.exchange";
    public static final String EXCHANGE_BILLING = "walletcentral.billing.exchange";
    public static final String EXCHANGE_NOTIFICATION = "walletcentral.notification.exchange";

    public static final String QUEUE_TRANSACTION = "walletcentral.transaction.queue";
    public static final String QUEUE_WALLET = "walletcentral.wallet.queue";
    public static final String QUEUE_WALLET_PLAN = "walletcentral.wallet.plan.queue";
    public static final String QUEUE_USAGE = "walletcentral.usage.queue";
    public static final String QUEUE_BILLING = "walletcentral.billing.queue";
    public static final String QUEUE_NOTIFICATION = "walletcentral.notification.queue";

    public static final String RK_TRANSACTION_BINDING = "transaction.binding";
    public static final String RK_WALLET_BINDING = "wallet.binding";
    public static final String RK_WALLET_PLAN_BINDING = "wallet.plan.binding";
    public static final String RK_USAGE_BINDING = "usage.binding";
    public static final String RK_BILLING_BINDING = "billing.binding";
    public static final String RK_NOTIFICATION_BINDING = "notification.binding";

    public static final String QUEUE_DLQ = "walletcentral.dlx";

    // ========== Exchanges ==========

    @Bean
    public TopicExchange transactionExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_TRANSACTION).durable(true).build();
    }

    @Bean
    public TopicExchange walletExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_WALLET).durable(true).build();
    }

    @Bean
    public TopicExchange usageExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_USAGE).durable(true).build();
    }

    @Bean
    public TopicExchange billingExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_BILLING).durable(true).build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NOTIFICATION).durable(true).build();
    }

    // ========== Queues ==========

    @Bean
    public Queue transactionProcessQueue() {
        return QueueBuilder.durable(QUEUE_TRANSACTION)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue walletPlanApproveQueue() {
        return QueueBuilder.durable(QUEUE_WALLET_PLAN)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue usageLogRecordQueue() {
        return QueueBuilder.durable(QUEUE_USAGE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Queue billingCompletedQueue() {
        return QueueBuilder.durable(QUEUE_BILLING)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue notificationPushQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    // ========== Bindings ==========

    @Bean
    public Binding transactionBinding() {
        return BindingBuilder.bind(transactionProcessQueue())
                .to(transactionExchange())
                .with(RK_TRANSACTION_BINDING);
    }

    @Bean
    public Binding walletPlanBinding() {
        return BindingBuilder.bind(walletPlanApproveQueue())
                .to(walletExchange())
                .with(RK_WALLET_PLAN_BINDING);
    }

    @Bean
    public Binding usageLogBinding() {
        return BindingBuilder.bind(usageLogRecordQueue())
                .to(usageExchange())
                .with(RK_USAGE_BINDING);
    }

    @Bean
    public Binding billingBinding() {
        return BindingBuilder.bind(billingCompletedQueue())
                .to(billingExchange())
                .with(RK_BILLING_BINDING);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationPushQueue())
                .to(notificationExchange())
                .with(RK_NOTIFICATION_BINDING);
    }

    // ========== Message Converter ==========

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
