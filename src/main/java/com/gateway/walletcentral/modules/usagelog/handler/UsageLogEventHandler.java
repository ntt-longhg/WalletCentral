package com.gateway.walletcentral.modules.usagelog.handler;

import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.billing.dto.BillingEvent;
import com.gateway.walletcentral.modules.servicecatalog.model.Service;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;
    private final MessageProducer messageProducer;

    public UsageLogEventHandler(UsageLogRepository usageLogRepository,
            WalletRepository walletRepository,
            TenantRepository tenantRepository,
            ServiceRepository serviceRepository,
            MessageProducer messageProducer) {
        this.usageLogRepository = usageLogRepository;
        this.walletRepository = walletRepository;
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
        this.messageProducer = messageProducer;
    }

    public void handleBillingUsageLog(Map<String, Object> message, Channel channel, long deliveryTag)
            throws IOException {
        String event = (String) message.get("event");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        BillingEvent billingEvent = convertToBillingEvent(payload);
        String referenceId = billingEvent.getReferenceId();
        log.info("Handling {} usage log - referenceId: {} | tenantId: {} | serviceCode: {}",
                event, referenceId, billingEvent.getTenantId(), billingEvent.getServiceCode());

        try {
            // Idempotency check
            if (usageLogRepository.existsByReferenceId(referenceId)) {
                log.info("UsageLog already exists for referenceId: {} - skipping", referenceId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Tenant tenant = tenantRepository.findById(UUID.fromString(billingEvent.getTenantId()))
                    .orElseThrow(() -> new RuntimeException("Tenant not found: " + billingEvent.getTenantId()));

            Service service = serviceRepository.findById(UUID.fromString(billingEvent.getServiceId()))
                    .orElseThrow(() -> new RuntimeException("Service not found: " + billingEvent.getServiceId()));

            // Create UsageLog
            UsageLog usageLog = UsageLog.builder()
                    .tenant(tenant)
                    .service(service)
                    .walletTypeSnapshot(WalletType.valueOf(billingEvent.getWalletType()))
                    .totalUsage(billingEvent.getUsageUnits())
                    .totalCharged(billingEvent.getTotalFee())
                    .creditLimitSnapshot(billingEvent.getCreditLimit())
                    .availableBalanceSnapshot(billingEvent.getAvailableBalanceAfter())
                    .feeBreakdown(billingEvent.getFeeBreakdown())
                    .referenceFrom(billingEvent.getReferenceFrom())
                    .referenceId(referenceId)
                    .createdAt(billingEvent.getCreatedAt() != null ? billingEvent.getCreatedAt() : OffsetDateTime.now())
                    .build();
            usageLogRepository.save(usageLog);
            log.info("Created UsageLog: id={} totalCharged={} for tenant={}",
                    usageLog.getId(), billingEvent.getTotalFee(), billingEvent.getTenantId());

            // Post-processing: audit logging, fee breakdown logging, low balance alert
            processUsageLogPostProcessing(usageLog, billingEvent);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("BILLING_WEBHOOK usage log handler error: referenceId={}", referenceId, e);
            throw new BusinessException("BILLING_WEBHOOK usage log handler error: " + e.getMessage());
        }
    }

    private void processUsageLogPostProcessing(UsageLog usageLog, BillingEvent event) {
        // Audit log
        auditLog.info(
                "USAGE_ID={} | TENANT={} | SERVICE={} | USAGE_UNITS={} | CHARGED={} | WALLET_TYPE={} | BALANCE_SNAPSHOT={}",
                usageLog.getId(), event.getTenantId(), event.getServiceCode(),
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
        Wallet wallet = walletRepository.findByTenantId(UUID.fromString(event.getTenantId())).orElse(null);
        if (wallet != null && wallet.getBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            log.warn("LOW BALANCE ALERT: tenant={} wallet={} balance={}",
                    event.getTenantId(), wallet.getId(), wallet.getBalance());
            Map<String, Object> notificationEvent = new HashMap<>();
            notificationEvent.put("tenantId", event.getTenantId());
            notificationEvent.put("type", "BILLING");
            notificationEvent.put("title", "Cảnh báo số dư thấp");
            notificationEvent.put("message", String.format("Ví %s số dư còn lại: %s VND",
                    wallet.getId(), wallet.getBalance()));
            notificationEvent.put("referenceType", "WALLET");
            notificationEvent.put("referenceId", wallet.getId().toString());
            messageProducer.publishNotification(notificationEvent);
        }
    }

    private BillingEvent convertToBillingEvent(Map<String, Object> map) {
        return BillingEvent.builder()
                .tenantId((String) map.get("tenantId"))
                .serviceId((String) map.get("serviceId"))
                .serviceCode((String) map.get("serviceCode"))
                .serviceName((String) map.get("serviceName"))
                .usageUnits((Integer) map.get("usageUnits"))
                .totalFee(new BigDecimal(map.get("totalFee").toString()))
                .walletId((String) map.get("walletId"))
                .walletType((String) map.get("walletType"))
                .balanceBefore(new BigDecimal(map.get("balanceBefore").toString()))
                .balanceAfter(new BigDecimal(map.get("balanceAfter").toString()))
                .creditLimit(new BigDecimal(map.get("creditLimit").toString()))
                .availableBalanceAfter(new BigDecimal(map.get("availableBalanceAfter").toString()))
                .referenceFrom((String) map.get("referenceFrom"))
                .referenceId((String) map.get("referenceId"))
                .description((String) map.get("description"))
                .webhookUrl((String) map.get("webhookUrl"))
                .webhookAuth((String) map.get("webhookAuth"))
                .build();
    }
}
