package com.gateway.walletcentral.modules.wallet.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.wallet.dto.*;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.model.WalletStatus;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.OffsetDateTime;

@Service
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final TenantRepository tenantRepository;

    public WalletService(WalletRepository walletRepository, TenantRepository tenantRepository) {
        this.walletRepository = walletRepository;
        this.tenantRepository = tenantRepository;
    }

    public WalletResponse create(WalletCreateRequest request) {
        if (walletRepository.existsByTenantId(request.getTenantId())) {
            throw new BusinessException("WALLET_EXISTS", "Wallet already exists for tenant: " + request.getTenantId());
        }

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.getTenantId()));

        Wallet wallet = Wallet.builder()
                .tenant(tenant)
                .type(request.getType())
                .balance(java.math.BigDecimal.ZERO)
                .creditLimit(java.math.BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        Wallet saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<WalletResponse> list(UUID tenantId, WalletType type, WalletStatus status, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = walletRepository.findWithCursor(cursorId, tenantId, type, status, pageable)
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
    public WalletResponse getById(UUID id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", id));
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getByTenantId(UUID tenantId) {
        Wallet wallet = walletRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", tenantId));
        return toResponse(wallet);
    }

    public WalletResponse updateStatus(UUID id, WalletStatusRequest request) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", id));
        wallet.setStatus(request.getStatus());
        wallet.setUpdatedAt(OffsetDateTime.now());
        Wallet saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    private WalletResponse toResponse(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .tenantId(w.getTenant().getId())
                .tenantName(w.getTenant().getName())
                .type(w.getType().name())
                .balance(w.getBalance())
                .creditLimit(w.getCreditLimit())
                .availableBalance(w.getAvailableBalance())
                .status(w.getStatus().name())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
