package com.gateway.walletcentral.modules.walletplan.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.event.WalletPlanApprovedEvent;
import com.gateway.walletcentral.core.event.WalletPlanCreatedEvent;
import com.gateway.walletcentral.core.event.WalletPlanRejectedEvent;
import com.gateway.walletcentral.modules.pricingplan.model.BonusType;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlan;
import com.gateway.walletcentral.modules.pricingplan.repository.PricingPlanRepository;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.modules.walletplan.dto.*;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlan;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import com.gateway.walletcentral.modules.walletplan.repository.WalletPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class WalletPlanService {

    private static final Logger log = LoggerFactory.getLogger(WalletPlanService.class);

    private final WalletPlanRepository walletPlanRepository;
    private final TenantRepository tenantRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WalletPlanService(WalletPlanRepository walletPlanRepository,
            TenantRepository tenantRepository,
            PricingPlanRepository pricingPlanRepository,
            WalletRepository walletRepository,
            ApplicationEventPublisher eventPublisher) {
        this.walletPlanRepository = walletPlanRepository;
        this.tenantRepository = tenantRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }

    public WalletPlanResponse create(WalletPlanCreateRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.getTenantId()));

        PricingPlan pricingPlan = pricingPlanRepository.findById(request.getPricingPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("PricingPlan", "id", request.getPricingPlanId()));

        var wallet = walletRepository.findByTenantId(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", request.getTenantId()));

        BigDecimal bonusAmount = calculateBonus(pricingPlan, pricingPlan.getPrice());
        BigDecimal creditedAmount = pricingPlan.getPrice().add(bonusAmount);

        WalletPlan walletPlan = WalletPlan.builder()
                .pricingPlan(pricingPlan)
                .tenant(tenant)
                .price(pricingPlan.getPrice())
                .bonusAmount(bonusAmount)
                .creditedAmount(creditedAmount)
                .status(WalletPlanStatus.PENDING)
                .createdBy(request.getCreatedBy())
                .createdAt(OffsetDateTime.now())
                .build();

        var saved = walletPlanRepository.save(walletPlan);

        WalletPlanCreatedEvent createdEvent = WalletPlanCreatedEvent.builder()
                .tenantId(walletPlan.getTenant().getId().toString())
                .walletPlanId(saved.getId().toString())
                .pricingPlanName(pricingPlan.getName())
                .createdBy(request.getCreatedBy())
                .build();
        eventPublisher.publishEvent(createdEvent);

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

    @Transactional
    public WalletPlanResponse approve(UUID id, WalletPlanApproveRequest request) {
        log.info("Approving wallet plan: {}", id);

        WalletPlan walletPlan = walletPlanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", id));

        if (walletPlan.getStatus() != WalletPlanStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Wallet plan must be PENDING to approve");
        }
        if (walletPlan.getApprovedBy() != null) {
            throw new BusinessException("INVALID_STATUS", "Wallet plan already approved");
        }

        walletPlan.setStatus(WalletPlanStatus.APPROVED);
        walletPlan.setApprovedAt(OffsetDateTime.now());
        walletPlan.setApprovedBy(request.getApprovedBy());

        var saved = walletPlanRepository.save(walletPlan);

        WalletPlanApprovedEvent approvedEvent = WalletPlanApprovedEvent.builder()
                .walletPlanId(saved.getId().toString())
                .tenantId(saved.getTenant().getId().toString())
                .pricingPlanName(saved.getPricingPlan().getName())
                .approvedBy(request.getApprovedBy())
                .build();
        eventPublisher.publishEvent(approvedEvent);

        return toResponse(saved);
    }

    public WalletPlanResponse reject(UUID id, WalletPlanApproveRequest request) {
        log.info("Rejecting wallet plan: {}", id);

        WalletPlan walletPlan = walletPlanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("WalletPlan", "id", id));

        if (walletPlan.getStatus() != WalletPlanStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Wallet plan must be PENDING to reject");
        }

        walletPlan.setStatus(WalletPlanStatus.REJECTED);
        walletPlan.setApprovedAt(OffsetDateTime.now());
        walletPlan.setApprovedBy(request.getApprovedBy());

        var saved = walletPlanRepository.save(walletPlan);

        WalletPlanRejectedEvent rejectedEvent = WalletPlanRejectedEvent.builder()
                .walletPlanId(saved.getId().toString())
                .tenantId(saved.getTenant().getId().toString())
                .pricingPlanName(saved.getPricingPlan().getName())
                .approvedBy(request.getApprovedBy())
                .build();
        eventPublisher.publishEvent(rejectedEvent);

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

    @Transactional(readOnly = true)
    public CursorPage<WalletPlanResponse> listPendingByTenant(UUID tenantId, CursorParams params) {
        var items = walletPlanRepository
                .findWithCursor(null, tenantId, WalletPlanStatus.PENDING, PageRequest.of(0, params.getSize() + 1))
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

    // private BigDecimal calculateNewCreditLimit(PricingPlan plan, BigDecimal
    // currentCreditLimit) {
    // return switch (plan.getCreditLimitAction()) {
    // case INCREASE -> currentCreditLimit.add(plan.getCreditLimitValue());
    // case SET -> plan.getCreditLimitValue();
    // case NONE -> currentCreditLimit;
    // };
    // }

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
                .status(wp.getStatus().name())
                .approvedAt(wp.getApprovedAt())
                .approvedBy(wp.getApprovedBy())
                .createdBy(wp.getCreatedBy())
                .createdAt(wp.getCreatedAt())
                .build();
    }
}
