package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class UsageLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(UsageLogConsumer.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.USAGELOG");

    private final UsageLogRepository usageLogRepository;
    private final NotificationService notificationService;

    public UsageLogConsumer(UsageLogRepository usageLogRepository, NotificationService notificationService) {
        this.usageLogRepository = usageLogRepository;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USAGE, executor = "virtualThreadExecutor")
    public void handleUsageLogRecorded(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        String usageLogId = (String) message.get("usageLogId");
        String tenantId = (String) message.get("tenantId");
        String serviceId = (String) message.get("serviceId");

        log.info("========== USAGE LOG CONSUMER START ==========");
        log.info("UsageLogId: {} | TenantId: {} | ServiceId: {}", usageLogId, tenantId, serviceId);

        try {
            UsageLog usageLog = usageLogRepository.findById(UUID.fromString(usageLogId)).orElse(null);
            if (usageLog == null) {
                log.error("UsageLog not found: {} - possible data inconsistency", usageLogId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String serviceName = usageLog.getService() != null ? usageLog.getService().getCode() : "TOPUP";
            String tenantName = usageLog.getTenant() != null ? usageLog.getTenant().getName() : "unknown";

            // Audit log
            auditLog.info(
                    "USAGE_ID={} | TENANT={} | SERVICE={} | USAGE_UNITS={} | CHARGED={} | WALLET_TYPE={} | BALANCE_SNAPSHOT={}",
                    usageLog.getId(), tenantName, serviceName,
                    usageLog.getTotalUsage(), usageLog.getTotalCharged(),
                    usageLog.getWalletTypeSnapshot(), usageLog.getAvailableBalanceSnapshot());

            // Fee breakdown logging
            if (usageLog.getFeeBreakdown() != null) {
                log.info("Fee breakdown: strategy={} initialFee={} subsequentFee={}",
                        usageLog.getFeeBreakdown().getStrategy(),
                        usageLog.getFeeBreakdown().getInitialFeeApplied(),
                        usageLog.getFeeBreakdown().getSubsequentFeeApplied());
            }

            // High usage alert → notification
            if (usageLog.getTotalUsage() > 10000) {
                log.warn("HIGH USAGE ALERT: tenant={} service={} usage={} charged={}",
                        tenantName, serviceName, usageLog.getTotalUsage(), usageLog.getTotalCharged());
                try {
                    notificationService.saveAndPush(
                            UUID.fromString(tenantId),
                            "USAGE",
                            "High Usage Alert",
                            String.format("Service %s: %d units used, charged %s VND",
                                    serviceName, usageLog.getTotalUsage(), usageLog.getTotalCharged()),
                            "USAGE_LOG",
                            usageLogId);
                } catch (Exception e) {
                    log.error("Failed to send high usage notification", e);
                }
            }

            // High charge alert → notification
            if (usageLog.getTotalCharged().compareTo(new BigDecimal("500000")) > 0) {
                log.warn("HIGH CHARGE ALERT: tenant={} service={} charged={}",
                        tenantName, serviceName, usageLog.getTotalCharged());
                try {
                    notificationService.saveAndPush(
                            UUID.fromString(tenantId),
                            "BILLING",
                            "High Charge Alert",
                            String.format("Service %s: charged %s VND",
                                    serviceName, usageLog.getTotalCharged()),
                            "USAGE_LOG",
                            usageLogId);
                } catch (Exception e) {
                    log.error("Failed to send high charge notification", e);
                }
            }

            log.info("========== USAGE LOG CONSUMER END ========== SUCCESS usageLog={}", usageLogId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("========== USAGE LOG CONSUMER END ========== FAILED usageLog={}", usageLogId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
