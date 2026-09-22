package com.gateway.walletcentral.modules.transaction.handler;

import com.gateway.walletcentral.modules.billing.dto.BillingEvent;
import com.gateway.walletcentral.modules.walletplan.dto.WalletPlanEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionEventHandler {

        private static final Logger log = LoggerFactory.getLogger(TransactionEventHandler.class);
        private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.TRANSACTION");

        private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000000");

        private final TransactionRepository transactionRepository;
        private final WalletRepository walletRepository;
        private final MessageProducer messageProducer;

        public TransactionEventHandler(TransactionRepository transactionRepository,
                        WalletRepository walletRepository,
                        MessageProducer messageProducer) {
                this.transactionRepository = transactionRepository;
                this.walletRepository = walletRepository;
                this.messageProducer = messageProducer;
        }

        @Transactional
        public void handleWalletPlanDeposit(Map<String, Object> message, Channel channel, long deliveryTag)
                        throws IOException {
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
                                channel.basicAck(deliveryTag, false);
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
                                        .createdAt(OffsetDateTime.now())
                                        .build();
                        transactionRepository.save(transaction);
                        log.info("Created DEPOSIT transaction: {} amount={} for wallet={}",
                                        transaction.getId(), walletPlanEvent.getAmount(),
                                        walletPlanEvent.getWalletId());

                        // Post-processing: reconciliation, audit, alerts
                        processTransactionPostProcessing(transaction, wallet);

                        channel.basicAck(deliveryTag, false);
                } catch (Exception e) {
                        log.error("WALLET_PLAN handler error: referenceId={}", referenceId, e);
                        throw new BusinessException("WALLET_PLAN handler error: " + e.getMessage());
                }
        }

        @Transactional
        public void handleBillingCharge(Map<String, Object> message, Channel channel, long deliveryTag)
                        throws IOException {

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
                                channel.basicAck(deliveryTag, false);
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
                                                        : OffsetDateTime.now())
                                        .build();
                        transactionRepository.save(transaction);
                        log.info("Created CHARGE transaction: {} amount={} for wallet={}",
                                        transaction.getId(), billingEvent.getTotalFee(), billingEvent.getWalletId());

                        // Post-processing: reconciliation, audit, alerts
                        processTransactionPostProcessing(transaction, wallet);

                        channel.basicAck(deliveryTag, false);
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

                // Large transaction alert
                if (transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) > 0) {
                        log.warn("LARGE TRANSACTION ALERT: id={} amount={} wallet={} tenant={}",
                                        transaction.getId(), transaction.getAmount(),
                                        wallet.getId(), wallet.getTenant().getName());
                        Map<String, Object> notificationEvent = new HashMap<>();
                        notificationEvent.put("tenantId", wallet.getTenant().getId().toString());
                        notificationEvent.put("type", "TRANSACTION");
                        notificationEvent.put("title", "Cảnh báo giao dịch lớn");
                        notificationEvent.put("message", String.format("Giao dịch %s với số tiền %s VND trên ví %s",
                                        transaction.getId(), transaction.getAmount(), wallet.getId()));
                        notificationEvent.put("referenceType", "TRANSACTION");
                        notificationEvent.put("referenceId", transaction.getId().toString());
                        messageProducer.publishNotification(notificationEvent);
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
