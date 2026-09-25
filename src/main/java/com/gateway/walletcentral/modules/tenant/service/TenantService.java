package com.gateway.walletcentral.modules.tenant.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.tenant.dto.*;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.model.TenantStatus;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantResponse create(TenantCreateRequest request) {
        if (tenantRepository.existsByClientId(request.getClientId())) {
            throw new BusinessException("DUPLICATE_CLIENT_ID", "Client ID already exists: " + request.getClientId());
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .allowedDomains(request.getAllowedDomains())
                .status(TenantStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<TenantResponse> list(TenantStatus status, String keyword, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = tenantRepository.findWithCursor(cursorId, status, keyword, pageable)
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
    public TenantResponse getById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
        return toResponse(tenant);
    }

    public TenantResponse update(UUID id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));

        if (request.getClientId() != null && tenantRepository.existsByClientIdAndIdNot(request.getClientId(), id)) {
            throw new BusinessException("DUPLICATE_CLIENT_ID", "Client ID already exists: " + request.getClientId());
        }

        if (request.getName() != null)
            tenant.setName(request.getName());
        if (request.getClientId() != null)
            tenant.setClientId(request.getClientId());
        if (request.getClientSecret() != null)
            tenant.setClientSecret(request.getClientSecret());
        if (request.getAllowedDomains() != null)
            tenant.setAllowedDomains(request.getAllowedDomains());
        tenant.setUpdatedAt(LocalDateTime.now());
        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    public TenantResponse updateStatus(UUID id, TenantStatusRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
        tenant.setStatus(request.getStatus());
        tenant.setUpdatedAt(LocalDateTime.now());
        Tenant saved = tenantRepository.save(tenant);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
        tenant.setDeletedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    private TenantResponse toResponse(Tenant t) {
        return TenantResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .clientId(t.getClientId())
                .allowedDomains(t.getAllowedDomains())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
