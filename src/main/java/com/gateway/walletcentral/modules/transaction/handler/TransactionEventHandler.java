package com.gateway.walletcentral.modules.transaction.handler;

import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.modules.billing.dto.BillingEvent;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.walletplan.dto.WalletPlanEvent;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
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
public class TransactionEventHandler {

        private static final Logger log = LoggerFactory.getLogger(TransactionEventHandler.class);
        private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.TRANSACTION");

        private static final BigDecimal DEFAULT_LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000000");

        private final TransactionRepository transactionRepository;
        private final WalletRepository walletRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final SystemConfigService configService;

        public TransactionEventHandler(TransactionRepository transactionRepository,
                        WalletRepository walletRepository,
                        ApplicationEventPublisher eventPublisher,
                        SystemConfigService configService) {
                this.transactionRepository = transactionRepository;
                this.walletRepository = walletRepository;
                this.eventPublisher = eventPublisher;
                this.configService = configService;
        }

        @Transactional
        public void handleWalletPlanDeposit(Map<String, Object> message) {
                String event = (String) message.get("event");
                Map<String, Object> payload = (Map<String, Object>) message.get("payload");
                WalletPlanEvent walletPlanEvent = convertToWalletPlanEvent(payload);
                String referenceId = walletPlanEvent.getReferenceId();
                log.info("Handling {} deposit - referenceId: {} | walletId: {}",
                                event, referenceId, walletPlanEvent.getWalletId());

                try {
                        // Idempotency check
                        if (transactionRepository.existsByReferenceId(referenceId)) {
                                log.info("Transaction already exists for referenceId: {} - skipping", referenceId);
                                return;
                        }

                        Wallet wallet = walletRepository
                                        .findByIdWithTenant(UUID.fromString(walletPlanEvent.getWalletId()))
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Wallet not found: " + walletPlanEvent.getWalletId()));

                        // Create Transaction (DEPOSIT)
                        Transaction transaction = Transaction.builder()
                                        .wallet(wallet)
                                        .amount(walletPlanEvent.getAmount())
                                        .type(TransactionType.DEPOSIT)
                                        .balanceBefore(walletPlanEvent.getBalanceBefore())
                                        .balanceAfter(walletPlanEvent.getBalanceAfter())
                                        .availableBalanceBefore(walletPlanEvent.getAvailableBalanceBefore())
                                        .availableBalanceAfter(walletPlanEvent.getAvailableBalanceAfter())
                                        .status(TransactionStatus.SUCCESS)
                                        .description(walletPlanEvent.getDescription())
                                        .referenceFrom(walletPlanEvent.getReferenceFrom())
                                        .referenceId(referenceId)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        transactionRepository.save(transaction);
                        log.info("Created DEPOSIT transaction: {} amount={} for wallet={}",
                                        transaction.getId(), walletPlanEvent.getAmount(),
                                        walletPlanEvent.getWalletId());

                        // Post-processing: reconciliation, audit, alerts
                        processTransactionPostProcessing(transaction, wallet);
                } catch (Exception e) {
                        log.error("WALLET_PLAN handler error: referenceId={}", referenceId, e);
                        throw new BusinessException("WALLET_PLAN handler error: " + e.getMessage());
                }
        }

        @Transactional
        public void handleRefundDeposit(Map<String, Object> message) {
                String event = (String) message.get("event");
                Map<String, Object> payload = (Map<String, Object>) message.get("payload");
                String referenceId = (String) payload.get("referenceId");
                String walletId = (String) payload.get("walletId");
                log.info("Handling {} refund - referenceId: {} | walletId: {}",
                                event, referenceId, walletId);

                try {
                        // Idempotency check
                        if (transactionRepository.existsByReferenceId(referenceId)) {
                                log.info("Transaction already exists for referenceId: {} - skipping", referenceId);
                                return;
                        }

                        Wallet wallet = walletRepository
                                        .findByIdWithTenant(UUID.fromString(walletId))
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Wallet not found: " + walletId));

                        // Create Transaction (REFUND)
                        Transaction transaction = Transaction.builder()
                                        .wallet(wallet)
                                        .amount(new BigDecimal(payload.get("amount").toString()))
                                        .type(TransactionType.REFUND)
                                        .balanceBefore(new BigDecimal(payload.get("balanceBefore").toString()))
                                        .balanceAfter(new BigDecimal(payload.get("balanceAfter").toString()))
                                        .availableBalanceBefore(
                                                        new BigDecimal(payload.get("availableBalanceBefore").toString()))
                                        .availableBalanceAfter(
                                                        new BigDecimal(payload.get("availableBalanceAfter").toString()))
                                        .status(TransactionStatus.SUCCESS)
                                        .description((String) payload.get("description"))
                                        .referenceFrom("REFUND")
                                        .referenceId(referenceId)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        transactionRepository.save(transaction);
                        log.info("Created REFUND transaction: {} amount={} for wallet={}",
                                        transaction.getId(), transaction.getAmount(), walletId);

                        // Post-processing: reconciliation, audit, alerts
                        processTransactionPostProcessing(transaction, wallet);
                } catch (Exception e) {
                        log.error("REFUND handler error: referenceId={}", referenceId, e);
                        throw new BusinessException("REFUND handler error: " + e.getMessage());
                }
        }

        @Transactional
        public void handleBillingCharge(Map<String, Object> message) {

                String event = (String) message.get("event");
                Map<String, Object> payload = (Map<String, Object>) message.get("payload");
                BillingEvent billingEvent = convertToBillingEvent(payload);
                String referenceId = billingEvent.getReferenceId();
                log.info("Handling {} charge - referenceId: {} | walletId: {} | totalFee: {}",
                                event, referenceId, billingEvent.getWalletId(), billingEvent.getTotalFee());

                try {
                        // Idempotency check
                        if (transactionRepository.existsByReferenceId(referenceId)) {
                                log.info("Transaction already exists for referenceId: {} - skipping", referenceId);
                                return;
                        }

                        Wallet wallet = walletRepository.findByIdWithTenant(UUID.fromString(billingEvent.getWalletId()))
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Wallet not found: " + billingEvent.getWalletId()));

                        // Create Transaction (CHARGE)
                        Transaction transaction = Transaction.builder()
                                        .wallet(wallet)
                                        .amount(billingEvent.getTotalFee())
                                        .type(TransactionType.CHARGE)
                                        .balanceBefore(billingEvent.getBalanceBefore())
                                        .balanceAfter(billingEvent.getBalanceAfter())
                                        .availableBalanceBefore(billingEvent.getBalanceBefore()
                                                        .add(billingEvent.getCreditLimit()))
                                        .availableBalanceAfter(billingEvent.getAvailableBalanceAfter())
                                        .status(TransactionStatus.SUCCESS)
                                        .description(billingEvent.getDescription())
                                        .referenceFrom(billingEvent.getReferenceFrom())
                                        .referenceId(referenceId)
                                        .createdAt(billingEvent.getCreatedAt() != null ? billingEvent.getCreatedAt()
                                                        : LocalDateTime.now())
                                        .build();
                        transactionRepository.save(transaction);
                        log.info("Created CHARGE transaction: {} amount={} for wallet={}",
                                        transaction.getId(), billingEvent.getTotalFee(), billingEvent.getWalletId());

                        // Post-processing: reconciliation, audit, alerts
                        processTransactionPostProcessing(transaction, wallet);
                } catch (Exception e) {
                        log.error("BILLING_WEBHOOK handler error: referenceId={}", referenceId, e);
                        throw new BusinessException("BILLING_WEBHOOK handler error: " + e.getMessage());
                }
        }

        private void processTransactionPostProcessing(Transaction transaction, Wallet wallet) {
                // Balance reconciliation
                BigDecimal expectedBalance = transaction.getBalanceAfter();
                BigDecimal actualBalance = wallet.getBalance();
                if (expectedBalance.compareTo(actualBalance) != 0) {
                        log.warn("BALANCE MISMATCH! TransactionId: {} | Expected: {} | Actual: {} | WalletId: {}",
                                        transaction.getId(), expectedBalance, actualBalance, wallet.getId());
                        wallet.setBalance(expectedBalance);
                        walletRepository.save(wallet);
                        log.info("Auto-rebalanced wallet {} to {}", wallet.getId(), expectedBalance);
                } else {
                        log.info("Balance verified OK: {} == {} for wallet={}", expectedBalance, actualBalance,
                                        wallet.getId());
                }

                // Audit log
                auditLog.info(
                                "TXN_ID={} | WALLET_ID={} | TYPE={} | AMOUNT={} | BALANCE_BEFORE={} | BALANCE_AFTER={} | STATUS={} | REF={} | REF_ID={}",
                                transaction.getId(), wallet.getId(), transaction.getType(),
                                transaction.getAmount(), transaction.getBalanceBefore(), transaction.getBalanceAfter(),
                                transaction.getStatus(), transaction.getReferenceFrom(), transaction.getReferenceId());

                // Large transaction alert (after DB commit, threshold is dynamic)
                BigDecimal largeThreshold = configService.getBigDecimal("alert.large_transaction_threshold",
                                DEFAULT_LARGE_TRANSACTION_THRESHOLD);
                if (transaction.getAmount().compareTo(largeThreshold) > 0) {
                        log.warn("LARGE TRANSACTION ALERT: id={} amount={} wallet={} tenant={}",
                                        transaction.getId(), transaction.getAmount(),
                                        wallet.getId(), wallet.getTenant().getName());
                        NotificationEvent notificationEvent = NotificationEvent.builder()
                                        .tenantId(wallet.getTenant().getId().toString())
                                        .type("TRANSACTION")
                                        .title(configService.getValue("alert.large_transaction_title",
                                                        "Cảnh báo giao dịch lớn"))
                                        .message(formatTemplate(
                                                        configService.getValue("alert.large_transaction_message",
                                                                        "Giao dịch {0} với số tiền {1} VND trên ví {2}"),
                                                        transaction.getId(), transaction.getAmount(), wallet.getId()))
                                        .referenceType("TRANSACTION")
                                        .referenceId(transaction.getId().toString())
                                        .build();
                        eventPublisher.publishEvent(notificationEvent);
                }

                // Transaction status verification
                if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                        log.warn("Non-success transaction detected: id={} status={}", transaction.getId(),
                                        transaction.getStatus());
                }
        }

        private WalletPlanEvent convertToWalletPlanEvent(Map<String, Object> map) {
                return WalletPlanEvent.builder()
                                .walletPlanId((String) map.get("walletPlanId"))
                                .tenantId((String) map.get("tenantId"))
                                .walletId((String) map.get("walletId"))
                                .amount(new BigDecimal(map.get("amount").toString()))
                                .balanceBefore(new BigDecimal(map.get("balanceBefore").toString()))
                                .balanceAfter(new BigDecimal(map.get("balanceAfter").toString()))
                                .availableBalanceBefore(new BigDecimal(map.get("availableBalanceBefore").toString()))
                                .availableBalanceAfter(new BigDecimal(map.get("availableBalanceAfter").toString()))
                                .description((String) map.get("description"))
                                .approvedBy((String) map.get("approvedBy"))
                                .referenceFrom((String) map.get("referenceFrom"))
                                .referenceId((String) map.get("referenceId"))
                                .build();
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

        /** Replaces {0}, {1}, ... placeholders in a config template. Never throws. */
        private String formatTemplate(String template, Object... args) {
                if (template == null) {
                        return null;
                }
                String result = template;
                for (int i = 0; i < args.length; i++) {
                        result = result.replace("{" + i + "}",
                                        args[i] == null ? "" : args[i].toString());
                }
                return result;
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
}
