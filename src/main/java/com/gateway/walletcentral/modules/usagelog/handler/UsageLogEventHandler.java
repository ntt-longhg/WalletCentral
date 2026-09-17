package com.gateway.walletcentral.modules.usagelog.handler;

import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class UsageLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UsageLogEventHandler.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.USAGELOG");

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100000");

    private final UsageLogRepository usageLogRepository;
    private final WalletRepository walletRepository;
    private final MessageProducer messageProducer;

    public UsageLogEventHandler(UsageLogRepository usageLogRepository,
            WalletRepository walletRepository,
            MessageProducer messageProducer) {
        this.usageLogRepository = usageLogRepository;
        this.walletRepository = walletRepository;
        this.messageProducer = messageProducer;
    }

    public void handleUsageLogRecorded(Map<String, Object> payload, Channel channel, long deliveryTag)
            throws IOException {
        String usageLogId = (String) payload.get("usageLogId");
        String tenantId = (String) payload.get("tenantId");
        log.info("UsageLog handler - usageLogId: {} | tenantId: {} | thread: {}", usageLogId, tenantId,
                Thread.currentThread());

        try {
            UsageLog usageLog = usageLogRepository.findById(UUID.fromString(usageLogId)).orElse(null);
            if (usageLog == null) {
                log.error("UsageLog not found: {} - possible data inconsistency", usageLogId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String serviceName = usageLog.getService() != null ? usageLog.getService().getCode() : "N/A";
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

            // Low balance alert
            Wallet wallet = walletRepository.findByTenantId(UUID.fromString(tenantId)).orElse(null);
            if (wallet != null && wallet.getBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
                log.warn("LOW BALANCE ALERT: tenant={} wallet={} balance={}",
                        tenantName, wallet.getId(), wallet.getBalance());
                Map<String, Object> notificationEvent = new HashMap<>();
                notificationEvent.put("tenantId", tenantId);
                notificationEvent.put("type", "BILLING");
                notificationEvent.put("title", "Cảnh báo số dư thấp");
                notificationEvent.put("message", String.format("Ví %s số dư còn lại: %s VND",
                        wallet.getId(), wallet.getBalance()));
                notificationEvent.put("referenceType", "WALLET");
                notificationEvent.put("referenceId", wallet.getId().toString());
                messageProducer.publishNotification(notificationEvent);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("UsageLog handler error: usageLogId={}", usageLogId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
