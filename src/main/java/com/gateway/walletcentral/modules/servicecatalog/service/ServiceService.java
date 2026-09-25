package com.gateway.walletcentral.modules.servicecatalog.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.servicecatalog.dto.*;
import com.gateway.walletcentral.modules.servicecatalog.model.PriceTier;
import com.gateway.walletcentral.modules.servicecatalog.model.ServicePrice;
import com.gateway.walletcentral.modules.servicecatalog.repository.PriceTierRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServicePriceRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
@Transactional
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final PriceTierRepository priceTierRepository;

    public ServiceService(ServiceRepository serviceRepository,
            ServicePriceRepository servicePriceRepository,
            PriceTierRepository priceTierRepository) {
        this.serviceRepository = serviceRepository;
        this.servicePriceRepository = servicePriceRepository;
        this.priceTierRepository = priceTierRepository;
    }

    // ========== Service CRUD ==========

    public ServiceResponse createService(ServiceCreateRequest request) {
        if (serviceRepository.existsByCode(request.getCode())) {
            throw new BusinessException("DUPLICATE_CODE", "Service code already exists: " + request.getCode());
        }

        com.gateway.walletcentral.modules.servicecatalog.model.Service service = com.gateway.walletcentral.modules.servicecatalog.model.Service
                .builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        var saved = serviceRepository.save(service);
        return toServiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<ServiceResponse> listServices(String keyword, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);
        var items = serviceRepository.findWithCursor(cursorId, keyword, pageable)
                .stream()
                .map(this::toServiceResponse)
                .toList();

        boolean hasNext = items.size() > params.getSize();
        if (hasNext) {
            items = items.subList(0, params.getSize());
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasNext, params.getSize());
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(UUID id) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        return toServiceResponse(service);
    }

    public ServiceResponse updateService(UUID id, ServiceUpdateRequest request) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        if (request.getCode() != null && serviceRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Service code already exists: " + request.getCode());
        }

        if (request.getCode() != null)
            service.setCode(request.getCode());
        if (request.getName() != null)
            service.setName(request.getName());
        if (request.getDescription() != null)
            service.setDescription(request.getDescription());
        service.setUpdatedAt(LocalDateTime.now());

        var saved = serviceRepository.save(service);
        return toServiceResponse(saved);
    }

    public void deleteService(UUID id) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        service.setDeletedAt(LocalDateTime.now());
        serviceRepository.save(service);
    }

    // ========== ServicePrice CRUD ==========

    public ServicePriceResponse createPrice(UUID serviceId, ServicePriceCreateRequest request) {
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        var price = ServicePrice.builder()
                .service(service)
                .initialSize(request.getInitialSize())
                .initialFee(request.getInitialFee())
                .subsequentSize(request.getSubsequentSize())
                .subsequentFee(request.getSubsequentFee())
                .isActive(true)
                .effectiveDate(request.getEffectiveDate())
                .createdAt(LocalDateTime.now())
                .build();

        var saved = servicePriceRepository.save(price);
        return toPriceResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<ServicePriceResponse> listPrices(UUID serviceId, Boolean activeOnly, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);
        var items = servicePriceRepository.findByServiceId(cursorId, serviceId, activeOnly, pageable)
                .stream()
                .map(this::toPriceResponse)
                .toList();

        boolean hasNext = items.size() > params.getSize();
        if (hasNext) {
            items = items.subList(0, params.getSize());
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasNext, params.getSize());
    }

    @Transactional(readOnly = true)
    public ServicePriceResponse getPriceById(UUID id) {
        var price = servicePriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicePrice", "id", id));
        return toPriceResponse(price);
    }

    public ServicePriceResponse updatePrice(UUID id, ServicePriceUpdateRequest request) {
        var price = servicePriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicePrice", "id", id));

        if (request.getInitialSize() != null)
            price.setInitialSize(request.getInitialSize());
        if (request.getInitialFee() != null)
            price.setInitialFee(request.getInitialFee());
        if (request.getSubsequentSize() != null)
            price.setSubsequentSize(request.getSubsequentSize());
        if (request.getSubsequentFee() != null)
            price.setSubsequentFee(request.getSubsequentFee());
        if (request.getEffectiveDate() != null)
            price.setEffectiveDate(request.getEffectiveDate());

        price.setUpdatedAt(LocalDateTime.now());

        var saved = servicePriceRepository.save(price);
        return toPriceResponse(saved);
    }

    public ServicePriceResponse toggleActive(UUID id) {
        var price = servicePriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicePrice", "id", id));
        price.setIsActive(!Boolean.TRUE.equals(price.getIsActive()));
        price.setUpdatedAt(LocalDateTime.now());
        var saved = servicePriceRepository.save(price);
        return toPriceResponse(saved);
    }

    // ========== PriceTier CRUD ==========

    public PriceTierResponse createTier(UUID priceId, PriceTierCreateRequest request) {
        var price = servicePriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("ServicePrice", "id", priceId));

        var tier = PriceTier.builder()
                .servicePrice(price)
                .tier(request.getTier())
                .basicFee(request.getBasicFee())
                .extendedSize(request.getExtendedSize())
                .extendedFee(request.getExtendedFee())
                .build();

        var saved = priceTierRepository.save(tier);
        return toTierResponse(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<PriceTierResponse> listTiers(UUID priceId) {
        return priceTierRepository.findByServicePriceIdOrderByTierAsc(priceId)
                .stream()
                .map(this::toTierResponse)
                .toList();
    }

    public PriceTierResponse updateTier(UUID tierId, PriceTierUpdateRequest request) {
        var tier = priceTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("PriceTier", "id", tierId));

        if (request.getBasicFee() != null)
            tier.setBasicFee(request.getBasicFee());
        if (request.getExtendedSize() != null)
            tier.setExtendedSize(request.getExtendedSize());
        if (request.getExtendedFee() != null)
            tier.setExtendedFee(request.getExtendedFee());

        var saved = priceTierRepository.save(tier);
        return toTierResponse(saved);
    }

    public void deleteTier(UUID tierId) {
        priceTierRepository.deleteById(tierId);
    }

    // ========== Mappers ==========

    private ServiceResponse toServiceResponse(com.gateway.walletcentral.modules.servicecatalog.model.Service s) {
        return ServiceResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ServicePriceResponse toPriceResponse(ServicePrice sp) {
        return ServicePriceResponse.builder()
                .id(sp.getId())
                .serviceId(sp.getService().getId())
                .serviceCode(sp.getService().getCode())
                .initialSize(sp.getInitialSize())
                .initialFee(sp.getInitialFee())
                .subsequentSize(sp.getSubsequentSize())
                .subsequentFee(sp.getSubsequentFee())
                .active(sp.getIsActive())
                .effectiveDate(sp.getEffectiveDate())
                .createdAt(sp.getCreatedAt())
                .build();
    }

    private PriceTierResponse toTierResponse(PriceTier pt) {
        return PriceTierResponse.builder()
                .id(pt.getId())
                .servicePriceId(pt.getServicePrice().getId())
                .tier(pt.getTier())
                .basicFee(pt.getBasicFee())
                .extendedSize(pt.getExtendedSize())
                .extendedFee(pt.getExtendedFee())
                .build();
    }
}
