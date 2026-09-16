package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustment;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustmentType;
import com.gateway.walletcentral.modules.creditadjustment.repository.CreditAdjustmentRepository;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlan;
import com.gateway.walletcentral.modules.walletplan.repository.WalletPlanRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WalletPlanConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanConsumer.class);
    // private static final Logger auditLog =
    // LoggerFactory.getLogger("AUDIT.WALLETPLAN");

    private final WalletPlanRepository walletPlanRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CreditAdjustmentRepository creditAdjustmentRepository;
    private final UsageLogRepository usageLogRepository;

    public WalletPlanConsumer(WalletPlanRepository walletPlanRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            CreditAdjustmentRepository creditAdjustmentRepository,
            UsageLogRepository usageLogRepository) {
        this.walletPlanRepository = walletPlanRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.creditAdjustmentRepository = creditAdjustmentRepository;
        this.usageLogRepository = usageLogRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WALLET_PLAN, executor = "virtualThreadExecutor")
    public void handleWalletPlanEvent(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        try {
            String event = (String) message.get("event");
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            log.info("Received wallet plan event: {}", event);
            switch (event) {
                case "APPROVED":
                    testAsync();
                    channel.basicAck(deliveryTag, false);
                    // handleWalletPlanApproved(message, deliveryTag, channel);
                    break;
                default:
                    log.warn("Unknown wallet plan event: {}", event);
                    channel.basicAck(deliveryTag, false);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing wallet plan event", e);
            channel.basicNack(deliveryTag, false, false);
        }

    }

    @Async("virtualThreadExecutor")
    public void testAsync() {
        try {
            Thread.sleep(10000);
            log.info("testAsync completed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // @RabbitListener(queues = RabbitMQConfig.QUEUE_WALLET_PLAN, executor =
    // "virtualThreadExecutor")
    public void handleWalletPlanApproved(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        String walletPlanId = (String) message.get("walletPlanId");
        String tenantId = (String) message.get("tenantId");
        String walletId = (String) message.get("walletId");
        String transactionId = message.get("transactionId") != null ? (String) message.get("transactionId") : null;

        log.info("========== WALLET PLAN CONSUMER START ==========");
        log.info("WalletPlanId: {} | TenantId: {} | WalletId: {}", walletPlanId, tenantId, walletId);

        try {
            WalletPlan walletPlan = walletPlanRepository.findByIdWithRelations(UUID.fromString(walletPlanId))
                    .orElse(null);
            if (walletPlan == null) {
                log.error("WalletPlan not found: {} - possible data inconsistency", walletPlanId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Wallet wallet = walletRepository.findById(UUID.fromString(walletId)).orElse(null);
            if (wallet == null) {
                log.error("Wallet not found: {} - possible data inconsistency", walletId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Balance reconciliation
            if (walletPlan.getBalanceAfter().compareTo(wallet.getBalance()) != 0) {
                log.warn("BALANCE MISMATCH after approval! Plan expects: {} | Actual: {} | WalletPlanId={}",
                        walletPlan.getBalanceAfter(), wallet.getBalance(), walletPlanId);
                wallet.setBalance(walletPlan.getBalanceAfter());
                wallet.setCreditLimit(walletPlan.getCreditLimitAfter());
                walletRepository.save(wallet);
                log.info("Auto-rebalanced wallet {} to balance={} creditLimit={}",
                        walletId, walletPlan.getBalanceAfter(), walletPlan.getCreditLimitAfter());
            }

            // Create Transaction if not already created by service
            if (transactionId == null) {
                BigDecimal creditedAmount = walletPlan.getCreditedAmount();
                Transaction transaction = Transaction.builder()
                        .wallet(wallet)
                        .amount(creditedAmount)
                        .type(TransactionType.DEPOSIT)
                        .balanceBefore(walletPlan.getBalanceBefore())
                        .balanceAfter(walletPlan.getBalanceAfter())
                        .availableBalanceBefore(walletPlan.getBalanceBefore().add(walletPlan.getCreditLimitBefore()))
                        .availableBalanceAfter(walletPlan.getBalanceAfter().add(walletPlan.getCreditLimitAfter()))
                        .status(TransactionStatus.SUCCESS)
                        .description("Wallet plan topup: " + walletPlan.getPricingPlan().getName())
                        .referenceFrom("WALLET_PLAN")
                        .referenceId(walletPlanId)
                        .createdAt(OffsetDateTime.now())
                        .build();
                transactionRepository.save(transaction);
                log.info("Created DEPOSIT transaction via consumer: {} amount={}", transaction.getId(), creditedAmount);
            }

            // Create CreditAdjustment if credit limit changed
            BigDecimal creditDiff = walletPlan.getCreditLimitAfter().subtract(walletPlan.getCreditLimitBefore());
            if (creditDiff.compareTo(BigDecimal.ZERO) != 0) {
                boolean exists = creditAdjustmentRepository.existsByWalletIdAndReferenceId(
                        wallet.getId(), walletPlanId);
                if (!exists) {
                    CreditAdjustmentType adjustmentType = creditDiff.compareTo(BigDecimal.ZERO) > 0
                            ? CreditAdjustmentType.INCREASE
                            : CreditAdjustmentType.DECREASE;
                    CreditAdjustment adjustment = CreditAdjustment.builder()
                            .wallet(wallet)
                            .creditLimitBefore(walletPlan.getCreditLimitBefore())
                            .creditLimitAfter(walletPlan.getCreditLimitAfter())
                            .adjustmentAmount(creditDiff.abs())
                            .type(adjustmentType)
                            .reason("Wallet plan: " + walletPlan.getPricingPlan().getName())
                            .referenceFrom("WALLET_PLAN")
                            .referenceId(walletPlanId)
                            .createdBy(walletPlan.getApprovedBy())
                            .createdAt(OffsetDateTime.now())
                            .build();
                    creditAdjustmentRepository.save(adjustment);
                    log.info("Created CreditAdjustment via consumer: {} type={} amount={}",
                            adjustment.getId(), adjustmentType, creditDiff.abs());
                }
            }

            // Create UsageLog if not already created
            boolean usageLogExists = usageLogRepository.existsByReferenceId(walletPlanId);
            if (!usageLogExists) {
                FeeBreakdownStructure feeBreakdown = new FeeBreakdownStructure();
                feeBreakdown.setStrategy("WALLET_PLAN_TOPUP");
                feeBreakdown.setInitialFeeApplied(BigDecimal.ZERO);
                feeBreakdown.setSubsequentFeeApplied(BigDecimal.ZERO);
                Map<String, Object> rawDetails = new HashMap<>();
                rawDetails.put("pricingPlanId", walletPlan.getPricingPlan().getId().toString());
                rawDetails.put("pricingPlanName", walletPlan.getPricingPlan().getName());
                rawDetails.put("price", walletPlan.getPrice());
                rawDetails.put("bonusAmount", walletPlan.getBonusAmount());
                rawDetails.put("creditedAmount", walletPlan.getCreditedAmount());
                feeBreakdown.setRawCalculationDetails(rawDetails);

                UsageLog usageLog = UsageLog.builder()
                        .tenant(walletPlan.getTenant())
                        .service(null)
                        .walletTypeSnapshot(wallet.getType())
                        .totalUsage(1)
                        .totalCharged(walletPlan.getCreditedAmount())
                        .creditLimitSnapshot(walletPlan.getCreditLimitAfter())
                        .availableBalanceSnapshot(walletPlan.getBalanceAfter().add(walletPlan.getCreditLimitAfter()))
                        .feeBreakdown(feeBreakdown)
                        .referenceFrom("WALLET_PLAN")
                        .referenceId(walletPlanId)
                        .createdAt(OffsetDateTime.now())
                        .build();
                usageLogRepository.save(usageLog);
                log.info("Created UsageLog via consumer: {} for wallet plan={}", usageLog.getId(), walletPlanId);
            }

            // Audit log
            auditLog.info("PLAN_ID={} | TENANT={} | PLAN={} | PRICE={} | BONUS={} | CREDITED={} | " +
                    "BALANCE_BEFORE={} | BALANCE_AFTER={} | CREDIT_BEFORE={} | CREDIT_AFTER={} | APPROVED_BY={}",
                    walletPlan.getId(), walletPlan.getTenant().getName(),
                    walletPlan.getPricingPlan().getName(), walletPlan.getPrice(),
                    walletPlan.getBonusAmount(), walletPlan.getCreditedAmount(),
                    walletPlan.getBalanceBefore(), walletPlan.getBalanceAfter(),
                    walletPlan.getCreditLimitBefore(), walletPlan.getCreditLimitAfter(),
                    walletPlan.getApprovedBy());

            log.info("========== WALLET PLAN CONSUMER END ========== SUCCESS plan={}", walletPlanId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("========== WALLET PLAN CONSUMER END ========== FAILED plan={}", walletPlanId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
