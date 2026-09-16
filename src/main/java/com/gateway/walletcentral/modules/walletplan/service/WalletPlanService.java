package com.gateway.walletcentral.modules.walletplan.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustment;
import com.gateway.walletcentral.modules.creditadjustment.model.CreditAdjustmentType;
import com.gateway.walletcentral.modules.creditadjustment.repository.CreditAdjustmentRepository;
import com.gateway.walletcentral.modules.pricingplan.model.BonusType;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlan;
import com.gateway.walletcentral.modules.pricingplan.repository.PricingPlanRepository;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.modules.walletplan.dto.*;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlan;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import com.gateway.walletcentral.modules.walletplan.repository.WalletPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class WalletPlanService {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanService.class);

    private final WalletPlanRepository walletPlanRepository;
    private final TenantRepository tenantRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CreditAdjustmentRepository creditAdjustmentRepository;
    private final UsageLogRepository usageLogRepository;
    private final MessageProducer messageProducer;

    public WalletPlanService(WalletPlanRepository walletPlanRepository,
            TenantRepository tenantRepository,
            PricingPlanRepository pricingPlanRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            CreditAdjustmentRepository creditAdjustmentRepository,
            UsageLogRepository usageLogRepository,
            MessageProducer messageProducer) {
        this.walletPlanRepository = walletPlanRepository;
        this.tenantRepository = tenantRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.creditAdjustmentRepository = creditAdjustmentRepository;
        this.usageLogRepository = usageLogRepository;
        this.messageProducer = messageProducer;
    }

    public WalletPlanResponse create(WalletPlanCreateRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.getTenantId()));

        PricingPlan pricingPlan = pricingPlanRepository.findById(request.getPricingPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("PricingPlan", "id", request.getPricingPlanId()));

        Wallet wallet = walletRepository.findByTenantId(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", request.getTenantId()));

        BigDecimal bonusAmount = calculateBonus(pricingPlan, pricingPlan.getPrice());
        BigDecimal creditedAmount = pricingPlan.getPrice().add(bonusAmount);
        BigDecimal newCreditLimit = calculateNewCreditLimit(pricingPlan, wallet.getCreditLimit());
        BigDecimal newBalance;
        if (wallet.getType() == com.gateway.walletcentral.modules.wallet.model.WalletType.POSTPAID) {
            newBalance = wallet.getBalance();
        } else {
            newBalance = wallet.getBalance().add(creditedAmount);
        }

        WalletPlan walletPlan = WalletPlan.builder()
                .pricingPlan(pricingPlan)
                .tenant(tenant)
                .price(pricingPlan.getPrice())
                .bonusAmount(bonusAmount)
                .creditedAmount(creditedAmount)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(newBalance)
                .creditLimitBefore(wallet.getCreditLimit())
                .creditLimitAfter(newCreditLimit)
                .status(WalletPlanStatus.PENDING)
                .createdBy(request.getCreatedBy())
                .createdAt(OffsetDateTime.now())
                .build();

        var saved = walletPlanRepository.save(walletPlan);
        // Notification event
        Map<String, Object> notifEvent = new HashMap<>();
        notifEvent.put("tenantId", walletPlan.getTenant().getId().toString());
        notifEvent.put("type", "WALLET_PLAN");
        notifEvent.put("title", "Wallet Plan Created");
        notifEvent.put("message", String.format("Wallet plan %s created successfully", pricingPlan.getName()));
        notifEvent.put("referenceType", "WALLET_PLAN");
        notifEvent.put("referenceId", saved.getId().toString());
        messageProducer.publishNotificationCreated(notifEvent);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<WalletPlanResponse> list(UUID tenantId, WalletPlanStatus status, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = walletPlanRepository.findWithCursor(cursorId, tenantId, status, pageable)
                .stream()
                .map(this::toResponse)
                .toList();

        boolean hasNext = items.size() > params.getSize();
        if (hasNext) {
            items = items.subList(0, params.getSize());
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasNext, params.getSize());
    }

    @Transactional(readOnly = true)
    public WalletPlanResponse getById(UUID id) {
        var walletPlan = walletPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", id));
        return toResponse(walletPlan);
    }

    public WalletPlanResponse approve(UUID id, WalletPlanApproveRequest request) {
        log.info("Approving wallet plan: {}", id);

        WalletPlan walletPlan = walletPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", id));

        if (walletPlan.getStatus() != WalletPlanStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Wallet plan must be PENDING to approve");
        }

        // Lock wallet with SELECT FOR UPDATE to prevent race conditions
        Wallet wallet = walletRepository.findByTenantIdForUpdate(walletPlan.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", walletPlan.getTenant().getId()));

        PricingPlan plan = walletPlan.getPricingPlan();
        BigDecimal bonusAmount = calculateBonus(plan, walletPlan.getPrice());
        BigDecimal creditedAmount = walletPlan.getPrice().add(bonusAmount);

        // Snapshot before
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal creditLimitBefore = wallet.getCreditLimit();
        BigDecimal availableBefore = wallet.getAvailableBalance();

        // Calculate new values based on wallet type
        BigDecimal newCreditLimit = calculateNewCreditLimit(plan, creditLimitBefore);
        BigDecimal newBalance;
        if (wallet.getType() == com.gateway.walletcentral.modules.wallet.model.WalletType.POSTPAID) {
            // POSTPAID: only update credit_limit, balance stays unchanged
            newBalance = balanceBefore;
        } else {
            // PREPAID: add credited amount to balance
            newBalance = balanceBefore.add(creditedAmount);
        }
        BigDecimal newAvailable = newBalance.add(newCreditLimit);

        // Update wallet
        wallet.setBalance(newBalance);
        wallet.setCreditLimit(newCreditLimit);
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepository.save(wallet);

        // 1. Create Transaction (DEPOSIT - topup into wallet)
        String refId = id.toString();
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(creditedAmount)
                .type(TransactionType.DEPOSIT)
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .availableBalanceBefore(availableBefore)
                .availableBalanceAfter(newAvailable)
                .status(TransactionStatus.SUCCESS)
                .description("Wallet plan topup: " + plan.getName() + " (bonus: " + bonusAmount + ")")
                .referenceFrom("WALLET_PLAN")
                .referenceId(refId)
                .createdAt(OffsetDateTime.now())
                .build();
        transactionRepository.save(transaction);
        log.info("Created DEPOSIT transaction: {} amount={} for wallet={}", transaction.getId(), creditedAmount,
                wallet.getId());

        // 2. Create CreditAdjustment (if credit limit changed)
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
                    .reason("Wallet plan: " + plan.getName() + " - Credit limit " + adjustmentType.name().toLowerCase())
                    .referenceFrom("WALLET_PLAN")
                    .referenceId(refId)
                    .createdBy(request.getApprovedBy())
                    .createdAt(OffsetDateTime.now())
                    .build();
            creditAdjustmentRepository.save(adjustment);
            log.info("Created CreditAdjustment: {} type={} amount={}", adjustment.getId(), adjustmentType,
                    creditDiff.abs());
        }

        // 3. Create UsageLog (record the plan purchase)
        FeeBreakdownStructure feeBreakdown = new FeeBreakdownStructure();
        feeBreakdown.setStrategy("WALLET_PLAN_TOPUP");
        feeBreakdown.setInitialFeeApplied(BigDecimal.ZERO);
        feeBreakdown.setSubsequentFeeApplied(BigDecimal.ZERO);
        Map<String, Object> rawDetails = new HashMap<>();
        rawDetails.put("pricingPlanId", plan.getId().toString());
        rawDetails.put("pricingPlanName", plan.getName());
        rawDetails.put("price", walletPlan.getPrice());
        rawDetails.put("bonusAmount", bonusAmount);
        rawDetails.put("creditedAmount", creditedAmount);
        rawDetails.put("creditLimitAction", plan.getCreditLimitAction().name());
        rawDetails.put("creditLimitValue", plan.getCreditLimitValue());
        feeBreakdown.setRawCalculationDetails(rawDetails);

        UsageLog usageLog = UsageLog.builder()
                .tenant(walletPlan.getTenant())
                .service(null) // No specific service for plan topup
                .walletTypeSnapshot(wallet.getType())
                .totalUsage(1)
                .totalCharged(creditedAmount)
                .creditLimitSnapshot(newCreditLimit)
                .availableBalanceSnapshot(newAvailable)
                .feeBreakdown(feeBreakdown)
                .referenceFrom("WALLET_PLAN")
                .referenceId(refId)
                .createdAt(OffsetDateTime.now())
                .build();
        usageLogRepository.save(usageLog);
        log.info("Created UsageLog: {} for wallet plan={}", usageLog.getId(), id);

        // Update wallet plan record
        walletPlan.setBonusAmount(bonusAmount);
        walletPlan.setCreditedAmount(creditedAmount);
        walletPlan.setBalanceBefore(balanceBefore);
        walletPlan.setBalanceAfter(newBalance);
        walletPlan.setCreditLimitBefore(creditLimitBefore);
        walletPlan.setCreditLimitAfter(newCreditLimit);
        walletPlan.setStatus(WalletPlanStatus.APPROVE);
        walletPlan.setApprovedAt(OffsetDateTime.now());
        walletPlan.setApprovedBy(request.getApprovedBy());

        var saved = walletPlanRepository.save(walletPlan);
        log.info("Wallet plan DB updates completed for id: {}", id);

        CompletableFuture.runAsync(() -> {
           try {
               // Publish event
               Map<String, Object> event = new HashMap<>();
               event.put("walletPlanId", saved.getId().toString());
               event.put("tenantId", walletPlan.getTenant().getId().toString());
               event.put("walletId", wallet.getId().toString());
               event.put("creditedAmount", creditedAmount);
               event.put("newBalance", wallet.getBalance());
               event.put("newCreditLimit", wallet.getCreditLimit());
               messageProducer.publishWalletPlanApproved(event);

               // Notification event
               Map<String, Object> notifEvent = new HashMap<>();
               notifEvent.put("tenantId", walletPlan.getTenant().getId().toString());
               notifEvent.put("type", "WALLET_PLAN");
               notifEvent.put("title", "Wallet Plan Approved");
               notifEvent.put("message", String.format("Wallet plan %s approved successfully", plan.getName()));
               notifEvent.put("referenceType", "WALLET_PLAN");
               notifEvent.put("referenceId", id.toString());
               messageProducer.publishNotificationCreated(notifEvent);
               log.info("Wallet plan approved: {} - transaction={}, usageLog={}", id, transaction.getId(), usageLog.getId());
           } catch (Exception e) {
               log.error("Failed to publish events for wallet plan: {}", id, e);
           }
        });
        return toResponse(saved);
    }

    public WalletPlanResponse reject(UUID id, WalletPlanApproveRequest request) {
        WalletPlan walletPlan = walletPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", id));

        if (walletPlan.getStatus() != WalletPlanStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Wallet plan must be PENDING to reject");
        }

        walletPlan.setStatus(WalletPlanStatus.REJECT);
        walletPlan.setApprovedAt(OffsetDateTime.now());
        walletPlan.setApprovedBy(request.getApprovedBy());

        var saved = walletPlanRepository.save(walletPlan);

        PricingPlan plan = walletPlan.getPricingPlan();

        // Notification event
        Map<String, Object> notifEvent = new HashMap<>();
        notifEvent.put("tenantId", walletPlan.getTenant().getId().toString());
        notifEvent.put("type", "WALLET_PLAN");
        notifEvent.put("title", "Wallet Plan Reject");
        notifEvent.put("message", String.format("Wallet plan %s reject successfully", plan.getName()));
        notifEvent.put("referenceType", "WALLET_PLAN");
        notifEvent.put("referenceId", saved.getId().toString());
        messageProducer.publishNotificationCreated(notifEvent);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<WalletPlanResponse> listPending(CursorParams params) {
        var items = walletPlanRepository.findByStatusOrderByCreatedAtAsc(WalletPlanStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();

        boolean hasNext = items.size() > params.getSize();
        if (hasNext) {
            items = items.subList(0, params.getSize());
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasNext, params.getSize());
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

    private WalletPlanResponse toResponse(WalletPlan wp) {
        return WalletPlanResponse.builder()
                .id(wp.getId())
                .tenantId(wp.getTenant().getId())
                .tenantName(wp.getTenant().getName())
                .pricingPlanId(wp.getPricingPlan().getId())
                .pricingPlanName(wp.getPricingPlan().getName())
                .price(wp.getPrice())
                .bonusAmount(wp.getBonusAmount())
                .creditedAmount(wp.getCreditedAmount())
                .balanceBefore(wp.getBalanceBefore())
                .balanceAfter(wp.getBalanceAfter())
                .creditLimitBefore(wp.getCreditLimitBefore())
                .creditLimitAfter(wp.getCreditLimitAfter())
                .status(wp.getStatus().name())
                .approvedAt(wp.getApprovedAt())
                .approvedBy(wp.getApprovedBy())
                .createdBy(wp.getCreatedBy())
                .createdAt(wp.getCreatedAt())
                .build();
    }
}
