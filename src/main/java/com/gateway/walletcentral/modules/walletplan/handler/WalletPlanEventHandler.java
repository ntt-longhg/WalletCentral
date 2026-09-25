package com.gateway.walletcentral.modules.walletplan.handler;

import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.modules.walletplan.dto.WalletPlanEvent;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustment;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustmentType;
import com.gateway.walletcentral.modules.creditadjustment.repository.CreditAdjustmentRepository;
import com.gateway.walletcentral.modules.pricingplan.model.BonusType;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlan;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlan;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import com.gateway.walletcentral.modules.walletplan.repository.WalletPlanRepository;
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
public class WalletPlanEventHandler {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanEventHandler.class);

    private final WalletPlanRepository walletPlanRepository;
    private final WalletRepository walletRepository;
    private final CreditAdjustmentRepository creditAdjustmentRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WalletPlanEventHandler(WalletPlanRepository walletPlanRepository,
            WalletRepository walletRepository,
            CreditAdjustmentRepository creditAdjustmentRepository,
            TransactionRepository transactionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.walletPlanRepository = walletPlanRepository;
        this.walletRepository = walletRepository;
        this.creditAdjustmentRepository = creditAdjustmentRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handlerApproved(Map<String, Object> payload) {
        log.info("========== WALLET PLAN HANDLER APPROVE START ==========");
        String walletPlanId = payload.get("id") != null ? payload.get("id").toString() : null;
        try {
            log.info("Wallet plan handler approve payload: {} | thread: {}", payload, Thread.currentThread());

            // Idempotency: skip if the downstream DEPOSIT transaction was already created
            // (covers redelivery after a previous successful run)
            if (walletPlanId != null && transactionRepository.existsByReferenceId(walletPlanId)) {
                log.info("Transaction already exists for walletPlanId: {} - skipping", walletPlanId);
                return;
            }

            WalletPlan walletPlan = walletPlanRepository.findByIdWithRelations(UUID.fromString(walletPlanId))
                    .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", walletPlanId));

            // Lock wallet with SELECT FOR UPDATE
            Wallet wallet = walletRepository
                    .findByTenantIdForUpdate(walletPlan.getTenant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId",
                            walletPlan.getTenant().getId()));

            PricingPlan plan = walletPlan.getPricingPlan();
            BigDecimal bonusAmount = calculateBonus(plan, walletPlan.getPrice());
            BigDecimal creditedAmount = walletPlan.getPrice().add(bonusAmount);

            // Snapshot before
            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal creditLimitBefore = wallet.getCreditLimit();
            BigDecimal availableBefore = wallet.getAvailableBalance();

            // Calculate new values
            BigDecimal newCreditLimit = calculateNewCreditLimit(plan, creditLimitBefore);
            BigDecimal newBalance;
            if (wallet.getType() == WalletType.POSTPAID) {
                newBalance = balanceBefore;
            } else {
                newBalance = balanceBefore.add(creditedAmount);
            }
            BigDecimal newAvailable = newBalance.add(newCreditLimit);

            // Update wallet
            wallet.setBalance(newBalance);
            wallet.setCreditLimit(newCreditLimit);
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);
            log.info("Wallet updated: balance {} -> {}, creditLimit {} -> {}",
                    balanceBefore, newBalance, creditLimitBefore, newCreditLimit);

            // Publish event for Transaction handler to create DEPOSIT transaction (after DB
            // commit via WalletPlanEventPublisher AFTER_COMMIT listener)
            WalletPlanEvent walletPlanEvent = WalletPlanEvent.builder()
                    .walletPlanId(walletPlanId)
                    .tenantId(walletPlan.getTenant().getId().toString())
                    .walletId(wallet.getId().toString())
                    .amount(creditedAmount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(newBalance)
                    .availableBalanceBefore(availableBefore)
                    .availableBalanceAfter(newAvailable)
                    .description("Mua gói dịch vụ " + plan.getName() + " (Khuyến mãi thêm: " + bonusAmount + ")")
                    .approvedBy(walletPlan.getApprovedBy())
                    .referenceFrom("WALLET_PLAN")
                    .referenceId(walletPlanId)
                    .build();
            eventPublisher.publishEvent(walletPlanEvent);
            log.info("Published WalletPlanEvent for wallet={}", wallet.getId());

            // Create CreditAdjustment (if credit limit changed)
            BigDecimal creditDiff = newCreditLimit.subtract(creditLimitBefore);
            if (creditDiff.compareTo(BigDecimal.ZERO) != 0) {
                CreditAdjustmentType adjustmentType = creditDiff.compareTo(BigDecimal.ZERO) > 0
                        ? CreditAdjustmentType.INCREASE
                        : CreditAdjustmentType.DECREASE;
                CreditAdjustment adjustment = CreditAdjustment.builder()
                        .wallet(wallet)
                        .creditLimitBefore(creditLimitBefore)
                        .creditLimitAfter(newCreditLimit)
                        .adjustmentAmount(creditDiff.abs())
                        .type(adjustmentType)
                        .reason("Mua gói dịch vụ " + plan.getName() + " - Hạn mức tín dụng "
                                + newCreditLimit)
                        .referenceFrom("WALLET_PLAN")
                        .referenceId(walletPlanId)
                        .createdBy(walletPlan.getApprovedBy())
                        .createdAt(LocalDateTime.now())
                        .build();
                creditAdjustmentRepository.save(adjustment);
                log.info("Created CreditAdjustment: {} type={} amount={}",
                        adjustment.getId(), adjustmentType, creditDiff.abs());
            }

            // Update wallet plan snapshots
            walletPlan.setBonusAmount(bonusAmount);
            walletPlan.setCreditedAmount(creditedAmount);
            walletPlanRepository.save(walletPlan);
            log.info("Wallet plan snapshots updated for id: {}", walletPlanId);

            // Notification event (after DB commit)
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .tenantId(walletPlan.getTenant().getId().toString())
                    .type("WALLET_PLAN")
                    .title("Gói dịch vụ được duyệt")
                    .message(String.format("Gói dịch vụ %s đã được duyệt thành công", plan.getName()))
                    .referenceType("WALLET_PLAN")
                    .referenceId(walletPlanId)
                    .build();
            eventPublisher.publishEvent(notificationEvent);

            log.info("========== WALLET PLAN HANDLER APPROVE END ========== SUCCESS plan={}", walletPlanId);
        } catch (RuntimeException e) {
            // Rethrow to roll back the transaction; the listener will nack (with one retry)
            log.error("========== WALLET PLAN HANDLER APPROVE END ========== FAILED plan={}", walletPlanId, e);
            throw e;
        }
    }

    @Transactional
    public void handlerRejected(Map<String, Object> payload) {
        log.info("========== WALLET PLAN HANDLER REJECT START ==========");
        String walletPlanId = payload.get("id") != null ? payload.get("id").toString() : null;
        try {
            log.info("Wallet plan handler reject payload: {} | thread: {}", payload, Thread.currentThread());

            WalletPlan walletPlan = walletPlanRepository.findByIdWithRelations(UUID.fromString(walletPlanId))
                    .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", walletPlanId));

            if (walletPlan.getStatus() != WalletPlanStatus.REJECTED) {
                log.warn("Wallet plan status is not REJECTED: {} - skipping", walletPlan.getStatus());
                return;
            }

            PricingPlan plan = walletPlan.getPricingPlan();

            // Notification event (after DB commit)
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .tenantId(walletPlan.getTenant().getId().toString())
                    .type("WALLET_PLAN")
                    .title("Gói dịch vụ bị từ chối")
                    .message(String.format("Gói dịch vụ %s đã bị từ chối", plan.getName()))
                    .referenceType("WALLET_PLAN")
                    .referenceId(walletPlanId)
                    .build();
            eventPublisher.publishEvent(notificationEvent);

            log.info("========== WALLET PLAN HANDLER REJECT END ========== SUCCESS plan={}", walletPlanId);
        } catch (RuntimeException e) {
            // Rethrow to roll back the transaction; the listener will nack (with one retry)
            log.error("========== WALLET PLAN HANDLER REJECT END ========== FAILED plan={}", walletPlanId, e);
            throw e;
        }
    }

    private BigDecimal calculateBonus(PricingPlan plan, BigDecimal price) {
        if (plan.getBonusType() == BonusType.PERCENTAGE && plan.getBonusValue() != null) {
            return price.multiply(plan.getBonusValue()).divide(BigDecimal.valueOf(100));
        } else if (plan.getBonusType() == BonusType.FIXED) {
            return plan.getBonusValue() != null ? plan.getBonusValue() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateNewCreditLimit(PricingPlan plan, BigDecimal currentCreditLimit) {
        return switch (plan.getCreditLimitAction()) {
            case INCREASE -> currentCreditLimit.add(plan.getCreditLimitValue());
            case SET -> plan.getCreditLimitValue();
            case NONE -> currentCreditLimit;
        };
    }
}
