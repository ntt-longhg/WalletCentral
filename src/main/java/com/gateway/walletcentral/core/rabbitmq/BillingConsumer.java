package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.billing.dto.BillingWebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
public class BillingConsumer {

    private static final Logger log = LoggerFactory.getLogger(BillingConsumer.class);

    private final RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public BillingConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BILLING, executor = "virtualThreadExecutor")
    public void handleBillingCompleted(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        String transactionId = (String) message.get("transactionId");
        String webhookUrl = (String) message.get("webhookUrl");
        String webhookAuth = (String) message.get("webhookAuth");

        log.info("========== BILLING CALLBACK START ==========");
        log.info("TransactionId: {}", transactionId);
        log.info("WebhookUrl: {}", webhookUrl);

        try {
            // Convert response to JSON
            Object responseObj = message.get("response");
            String responseBody = objectMapper.writeValueAsString(responseObj);
            log.info("Response payload: {}", responseBody);

            // Call webhook URL
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (webhookAuth != null && !webhookAuth.isBlank()) {
                headers.set("Authorization", webhookAuth);
                log.info("Webhook auth header set");
            }

            HttpEntity<String> entity = new HttpEntity<>(responseBody, headers);
            ResponseEntity<String> webhookResponse = restTemplate.postForEntity(webhookUrl, entity, String.class);

            log.info("Webhook callback response: status={} body={}", webhookResponse.getStatusCode(),
                    webhookResponse.getBody());
            log.info("========== BILLING CALLBACK END ========== SUCCESS");

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to call webhook URL: {} for transaction: {}", webhookUrl, transactionId, e);
            log.info("========== BILLING CALLBACK END ========== FAILED");
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
