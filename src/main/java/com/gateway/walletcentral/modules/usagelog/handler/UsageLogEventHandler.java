package com.gateway.walletcentral.modules.usagelog.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.modules.billing.dto.BillingEvent;
import com.gateway.walletcentral.modules.servicecatalog.model.Service;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class UsageLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UsageLogEventHandler.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.USAGELOG");

    private static final BigDecimal DEFAULT_LOW_BALANCE_THRESHOLD = new BigDecimal("100000");

    private final UsageLogRepository usageLogRepository;
    private final WalletRepository walletRepository;
    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final SystemConfigService configService;

    public UsageLogEventHandler(UsageLogRepository usageLogRepository,
            WalletRepository walletRepository,
            TenantRepository tenantRepository,
            ServiceRepository serviceRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            SystemConfigService configService) {
        this.usageLogRepository = usageLogRepository;
        this.walletRepository = walletRepository;
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.configService = configService;
    }

    @Transactional
    public void handleBillingUsageLog(Map<String, Object> message) {
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
                    .createdAt(billingEvent.getCreatedAt() != null ? billingEvent.getCreatedAt() : LocalDateTime.now())
                    .build();
            usageLogRepository.save(usageLog);
            log.info("Created UsageLog: id={} totalCharged={} for tenant={}",
                    usageLog.getId(), billingEvent.getTotalFee(), billingEvent.getTenantId());

            // Post-processing: audit logging, fee breakdown logging, low balance alert
            processUsageLogPostProcessing(usageLog, billingEvent);
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

        // Low balance alert (threshold is dynamic, published after DB commit)
        BigDecimal lowThreshold = configService.getBigDecimal("alert.low_balance_threshold",
                DEFAULT_LOW_BALANCE_THRESHOLD);
        Wallet wallet = walletRepository.findByTenantId(UUID.fromString(event.getTenantId())).orElse(null);
        if (wallet != null && wallet.getBalance().compareTo(lowThreshold) < 0) {
            log.warn("LOW BALANCE ALERT: tenant={} wallet={} balance={}",
                    event.getTenantId(), wallet.getId(), wallet.getBalance());
            String messageTemplate = configService.getValue("alert.low_balance_message",
                    "Ví {0} số dư còn lại: {1} VND");
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .tenantId(event.getTenantId())
                    .type("BILLING")
                    .title(configService.getValue("alert.low_balance_title", "Cảnh báo số dư thấp"))
                    .message(messageTemplate.replace("{0}", wallet.getId().toString())
                            .replace("{1}", String.valueOf(wallet.getBalance())))
                    .referenceType("WALLET")
                    .referenceId(wallet.getId().toString())
                    .build();
            eventPublisher.publishEvent(notificationEvent);
        }
    }

    private BillingEvent convertToBillingEvent(Map<String, Object> map) {
        return BillingEvent.builder()
                .tenantId((String) map.get("tenantId"))
                .serviceId((String) map.get("serviceId"))
                .serviceCode((String) map.get("serviceCode"))
                .serviceName((String) map.get("serviceName"))
                .usageUnits(toInteger(map.get("usageUnits")))
                .totalFee(new BigDecimal(map.get("totalFee").toString()))
                .walletId((String) map.get("walletId"))
                .walletType((String) map.get("walletType"))
                .balanceBefore(new BigDecimal(map.get("balanceBefore").toString()))
                .balanceAfter(new BigDecimal(map.get("balanceAfter").toString()))
                .creditLimit(new BigDecimal(map.get("creditLimit").toString()))
                .availableBalanceAfter(new BigDecimal(map.get("availableBalanceAfter").toString()))
                .feeBreakdown(toFeeBreakdown(map.get("feeBreakdown")))
                .referenceFrom((String) map.get("referenceFrom"))
                .referenceId((String) map.get("referenceId"))
                .description((String) map.get("description"))
                .createdAt(toLocalDateTime(map.get("createdAt")))
                .webhookUrl((String) map.get("webhookUrl"))
                .webhookAuth((String) map.get("webhookAuth"))
                .build();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return LocalDateTime.parse(value.toString());
    }

    private FeeBreakdownStructure toFeeBreakdown(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof FeeBreakdownStructure structure) {
            return structure;
        }
        return objectMapper.convertValue(value, FeeBreakdownStructure.class);
    }
}
