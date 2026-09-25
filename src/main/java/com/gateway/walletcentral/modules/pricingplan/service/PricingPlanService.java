package com.gateway.walletcentral.modules.pricingplan.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.pricingplan.dto.*;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlan;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlanStatus;
import com.gateway.walletcentral.modules.pricingplan.model.PricingPlanType;
import com.gateway.walletcentral.modules.pricingplan.repository.PricingPlanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
@Transactional
public class PricingPlanService {

    private final PricingPlanRepository pricingPlanRepository;

    public PricingPlanService(PricingPlanRepository pricingPlanRepository) {
        this.pricingPlanRepository = pricingPlanRepository;
    }

    public PricingPlanResponse create(PricingPlanCreateRequest request) {
        if (pricingPlanRepository.existsByCode(request.getCode())) {
            throw new BusinessException("DUPLICATE_CODE", "Pricing plan code already exists: " + request.getCode());
        }

        PricingPlan plan = PricingPlan.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .type(request.getType())
                .bonusType(request.getBonusType())
                .bonusValue(request.getBonusValue())
                .creditLimitAction(request.getCreditLimitAction())
                .creditLimitValue(request.getCreditLimitValue())
                .status(PricingPlanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        var saved = pricingPlanRepository.save(plan);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<PricingPlanResponse> list(PricingPlanType type, PricingPlanStatus status, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = pricingPlanRepository.findWithCursor(cursorId, type, status, pageable)
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
    public PricingPlanResponse getById(UUID id) {
        var plan = pricingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PricingPlan", "id", id));
        return toResponse(plan);
    }

    public PricingPlanResponse update(UUID id, PricingPlanUpdateRequest request) {
        var plan = pricingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PricingPlan", "id", id));

        if (request.getName() != null)
            plan.setName(request.getName());
        if (request.getDescription() != null)
            plan.setDescription(request.getDescription());
        if (request.getPrice() != null)
            plan.setPrice(request.getPrice());
        if (request.getBonusType() != null)
            plan.setBonusType(request.getBonusType());
        if (request.getBonusValue() != null)
            plan.setBonusValue(request.getBonusValue());
        if (request.getCreditLimitAction() != null)
            plan.setCreditLimitAction(request.getCreditLimitAction());
        if (request.getCreditLimitValue() != null)
            plan.setCreditLimitValue(request.getCreditLimitValue());

        plan.setUpdatedAt(LocalDateTime.now());

        var saved = pricingPlanRepository.save(plan);
        return toResponse(saved);
    }

    public PricingPlanResponse updateStatus(UUID id, PricingPlanStatusRequest request) {
        var plan = pricingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PricingPlan", "id", id));
        plan.setStatus(request.getStatus());
        plan.setUpdatedAt(LocalDateTime.now());
        var saved = pricingPlanRepository.save(plan);
        return toResponse(saved);
    }

    private PricingPlanResponse toResponse(PricingPlan p) {
        return PricingPlanResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .type(p.getType().name())
                .bonusType(p.getBonusType().name())
                .bonusValue(p.getBonusValue())
                .creditLimitAction(p.getCreditLimitAction().name())
                .creditLimitValue(p.getCreditLimitValue())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
