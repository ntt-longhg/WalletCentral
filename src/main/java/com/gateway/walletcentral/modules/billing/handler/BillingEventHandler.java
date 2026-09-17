package com.gateway.walletcentral.modules.billing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
public class BillingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BillingEventHandler.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BillingEventHandler(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void handleBillingCallback(Map<String, Object> payload, Channel channel, long deliveryTag) throws IOException {
        String transactionId = (String) payload.get("transactionId");
        String webhookUrl = (String) payload.get("webhookUrl");
        String webhookAuth = (String) payload.get("webhookAuth");
        log.info("Billing handler - transactionId: {} | webhookUrl: {} | thread: {}", transactionId, webhookUrl,
                Thread.currentThread());

        try {
            Object responseObj = payload.get("response");
            String responseBody = objectMapper.writeValueAsString(responseObj);
            log.info("Response payload: {}", responseBody);

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

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to call webhook URL: {} for transaction: {}", webhookUrl, transactionId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
