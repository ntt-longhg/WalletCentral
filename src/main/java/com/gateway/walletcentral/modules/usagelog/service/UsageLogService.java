package com.gateway.walletcentral.modules.usagelog.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.usagelog.dto.*;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
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
public class UsageLogService {

        private final UsageLogRepository usageLogRepository;
        private final TenantRepository tenantRepository;
        private final com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository serviceRepository;
        private final WalletRepository walletRepository;
        private final MessageProducer messageProducer;

        public UsageLogService(UsageLogRepository usageLogRepository,
                        TenantRepository tenantRepository,
                        com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository serviceRepository,
                        WalletRepository walletRepository,
                        MessageProducer messageProducer) {
                this.usageLogRepository = usageLogRepository;
                this.tenantRepository = tenantRepository;
                this.serviceRepository = serviceRepository;
                this.walletRepository = walletRepository;
                this.messageProducer = messageProducer;
        }

        public UsageLogResponse create(UsageLogCreateRequest request) {
                var tenant = tenantRepository.findById(request.getTenantId())
                                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id",
                                                request.getTenantId()));

                var service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new ResourceNotFoundException("Service", "id",
                                                request.getServiceId()));

                var wallet = walletRepository.findByTenantId(request.getTenantId())
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId",
                                                request.getTenantId()));

                UsageLog usageLog = UsageLog.builder()
                                .tenant(tenant)
                                .service(service)
                                .walletTypeSnapshot(wallet.getType())
                                .totalUsage(request.getTotalUsage())
                                .totalCharged(BigDecimal.ZERO)
                                .creditLimitSnapshot(wallet.getCreditLimit())
                                .availableBalanceSnapshot(wallet.getAvailableBalance())
                                .referenceFrom(request.getReferenceFrom())
                                .referenceId(request.getReferenceId())
                                .createdAt(OffsetDateTime.now())
                                .build();

                var saved = usageLogRepository.save(usageLog);

                Map<String, Object> event = new HashMap<>();
                event.put("usageLogId", saved.getId().toString());
                event.put("tenantId", request.getTenantId().toString());
                event.put("serviceId", request.getServiceId().toString());
                event.put("totalUsage", request.getTotalUsage());
                event.put("walletId", wallet.getId().toString());
                messageProducer.publishUsageLog(event);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public CursorPage<UsageLogResponse> list(UUID tenantId, UUID serviceId, CursorParams params) {
                UUID cursorId = CursorUtil.parseCursor(params.getCursor());
                var pageable = PageRequest.of(0, params.getSize() + 1);

                var items = usageLogRepository.findWithCursor(cursorId, tenantId, serviceId, pageable)
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
        public UsageLogResponse getById(UUID id) {
                var usageLog = usageLogRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("UsageLog", "id", id));
                return toResponse(usageLog);
        }

        private UsageLogResponse toResponse(UsageLog ul) {
                return UsageLogResponse.builder()
                                .id(ul.getId())
                                .tenantId(ul.getTenant().getId())
                                .tenantName(ul.getTenant().getName())
                                .serviceId(ul.getService() != null ? ul.getService().getId() : null)
                                .serviceCode(ul.getService() != null ? ul.getService().getCode() : null)
                                .walletTypeSnapshot(ul.getWalletTypeSnapshot().name())
                                .totalUsage(ul.getTotalUsage())
                                .totalCharged(ul.getTotalCharged())
                                .creditLimitSnapshot(ul.getCreditLimitSnapshot())
                                .availableBalanceSnapshot(ul.getAvailableBalanceSnapshot())
                                .feeBreakdown(ul.getFeeBreakdown() != null ? Map.of(
                                                "strategy",
                                                ul.getFeeBreakdown().getStrategy() != null
                                                                ? ul.getFeeBreakdown().getStrategy()
                                                                : "",
                                                "initialFeeApplied",
                                                ul.getFeeBreakdown().getInitialFeeApplied() != null
                                                                ? ul.getFeeBreakdown().getInitialFeeApplied()
                                                                : BigDecimal.ZERO,
                                                "subsequentFeeApplied",
                                                ul.getFeeBreakdown().getSubsequentFeeApplied() != null
                                                                ? ul.getFeeBreakdown().getSubsequentFeeApplied()
                                                                : BigDecimal.ZERO)
                                                : new HashMap<>())
                                .referenceFrom(ul.getReferenceFrom())
                                .referenceId(ul.getReferenceId())
                                .createdAt(ul.getCreatedAt())
                                .build();
        }
}
