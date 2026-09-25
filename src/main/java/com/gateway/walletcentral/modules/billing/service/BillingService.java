package com.gateway.walletcentral.modules.billing.service;

import com.gateway.walletcentral.core.event.BillingProcessedEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.billing.dto.BillingWebhookRequest;
import com.gateway.walletcentral.modules.billing.dto.BillingWebhookResponse;
import com.gateway.walletcentral.modules.servicecatalog.model.Service;
import com.gateway.walletcentral.modules.servicecatalog.model.ServicePrice;
import com.gateway.walletcentral.modules.servicecatalog.repository.PriceTierRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServicePriceRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.stereotype.Service
@Transactional
public class BillingService {

        private static final Logger log = LoggerFactory.getLogger(BillingService.class);

        private final ServiceRepository serviceRepository;
        private final ServicePriceRepository servicePriceRepository;
        private final PriceTierRepository priceTierRepository;
        private final WalletRepository walletRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final SystemConfigService configService;

        public BillingService(ServiceRepository serviceRepository,
                        ServicePriceRepository servicePriceRepository,
                        PriceTierRepository priceTierRepository,
                        WalletRepository walletRepository,
                        ApplicationEventPublisher eventPublisher,
                        SystemConfigService configService) {
                this.serviceRepository = serviceRepository;
                this.servicePriceRepository = servicePriceRepository;
                this.priceTierRepository = priceTierRepository;
                this.walletRepository = walletRepository;
                this.eventPublisher = eventPublisher;
                this.configService = configService;
        }

        /**
         * Process billing webhook request.
         * Uses SELECT FOR UPDATE to lock wallet and prevent race conditions.
         * All operations are within a single transaction - rolls back on any failure.
         *
         * @param tenant  the authenticated tenant from X-API-Key
         * @param request the billing webhook request
         * @return billing response with transaction details
         */
        public BillingWebhookResponse processBilling(Tenant tenant, BillingWebhookRequest request) {
                log.info("========== BILLING PROCESS START ==========");
                log.info("Tenant: {} (id={})", tenant.getName(), tenant.getId());
                log.info("ServiceCode: {} | UsageUnits: {} | RefId: {}",
                                request.getServiceCode(), request.getUsageUnits(), request.getReferenceId());

                // 1. Find and validate service
                Service service = serviceRepository.findByCode(request.getServiceCode())
                                .orElseThrow(() -> {
                                        log.error("Service not found: {}", request.getServiceCode());
                                        return new ResourceNotFoundException("Service", "code",
                                                        request.getServiceCode());
                                });
                log.info("Service found: {} (id={})", service.getName(), service.getId());

                // 2. Find active service price
                List<ServicePrice> activePrices = servicePriceRepository
                                .findByServiceIdAndIsActiveTrue(service.getId(),
                                                LocalDateTime.now(),
                                                PageRequest.of(0, 1));
                if (activePrices.isEmpty()) {
                        log.error("No active price found for service: {}", service.getCode());
                        throw new BusinessException("NO_ACTIVE_PRICE",
                                        "No active price found for service: " + service.getCode());
                }
                ServicePrice servicePrice = activePrices.getFirst();
                log.info("ServicePrice found: id={} initialFee={} initialSize={} subsequentFee={} subsequentSize={}",
                                servicePrice.getId(), servicePrice.getInitialFee(), servicePrice.getInitialSize(),
                                servicePrice.getSubsequentFee(), servicePrice.getSubsequentSize());

                // 3. Find and lock wallet (SELECT FOR UPDATE)
                Wallet wallet = walletRepository.findByTenantIdForUpdate(tenant.getId())
                                .orElseThrow(() -> {
                                        log.error("Wallet not found for tenant: {}", tenant.getId());
                                        return new ResourceNotFoundException("Wallet", "tenantId", tenant.getId());
                                });
                log.info("Wallet locked (SELECT FOR UPDATE): id={} balance={} creditLimit={} available={}",
                                wallet.getId(), wallet.getBalance(), wallet.getCreditLimit(),
                                wallet.getAvailableBalance());

                // 4. Calculate fee
                BigDecimal totalFee = calculateFee(servicePrice, request.getUsageUnits());
                log.info("Fee calculated: {} for {} units", totalFee, request.getUsageUnits());

                // 5. Check sufficient balance
                if (wallet.getAvailableBalance().compareTo(totalFee) < 0) {
                        log.error("Insufficient balance: available={} required={}", wallet.getAvailableBalance(),
                                        totalFee);
                        throw new BusinessException("INSUFFICIENT_BALANCE",
                                        "Insufficient balance. Available: " + wallet.getAvailableBalance()
                                                        + ", Required: " + totalFee);
                }

                // 6. Snapshot before
                BigDecimal balanceBefore = wallet.getBalance();
                BigDecimal availableBefore = wallet.getAvailableBalance();
                BigDecimal newBalance = balanceBefore.subtract(totalFee);
                BigDecimal newAvailable = newBalance.add(wallet.getCreditLimit());

                // 7. Deduct from wallet
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
                log.info("Wallet updated: balance {} -> {} , available {} -> {}",
                                balanceBefore, newBalance, availableBefore, newAvailable);

                // 8. Generate reference ID and build fee breakdown
                String refId = UUID.randomUUID().toString();
                FeeBreakdownStructure feeBreakdownStructure = buildFeeBreakdown(servicePrice, request.getUsageUnits(),
                                totalFee);
                LocalDateTime now = LocalDateTime.now();

                // 9. Publish event after DB commit via ApplicationEvent (ensures DB consistency)
                BillingProcessedEvent billingProcessedEvent = BillingProcessedEvent.builder()
                                .tenantId(tenant.getId().toString())
                                .serviceId(service.getId().toString())
                                .serviceCode(service.getCode())
                                .serviceName(service.getName())
                                .usageUnits(request.getUsageUnits())
                                .totalFee(totalFee)
                                .walletId(wallet.getId().toString())
                                .walletType(wallet.getType().name())
                                .balanceBefore(balanceBefore)
                                .balanceAfter(newBalance)
                                .creditLimit(wallet.getCreditLimit())
                                .availableBalanceAfter(newAvailable)
                                .feeBreakdown(feeBreakdownStructure)
                                .referenceFrom("BILLING_WEBHOOK")
                                .referenceId(refId)
                                .createdAt(now)
                                .description(request.getDescription() != null ? request.getDescription()
                                                : configService.getValue("billing.default_description_template",
                                                                "Thanh toán cước {0}").replace("{0}",
                                                                                service.getCode()))
                                .webhookUrl(request.getWebhookUrl())
                                .webhookAuth(request.getWebhookAuth())
                                .metadata(request.getMetadata())
                                .build();
                eventPublisher.publishEvent(billingProcessedEvent);
                log.info("Published BillingProcessedEvent after commit: refId={}", refId);

                // 10. Build response
                BillingWebhookResponse.FeeBreakdownDto feeBreakdownDto = BillingWebhookResponse.FeeBreakdownDto
                                .builder()
                                .strategy(feeBreakdownStructure.getStrategy())
                                .initialFee(feeBreakdownStructure.getInitialFeeApplied())
                                .initialUnits(servicePrice.getInitialSize())
                                .subsequentFee(feeBreakdownStructure.getSubsequentFeeApplied())
                                .subsequentUnits(request.getUsageUnits() > servicePrice.getInitialSize()
                                                ? request.getUsageUnits() - servicePrice.getInitialSize()
                                                : 0)
                                .totalFee(totalFee)
                                .build();

                BillingWebhookResponse response = BillingWebhookResponse.builder()
                                .transactionId(refId)
                                .tenantId(tenant.getId().toString())
                                .serviceCode(service.getCode())
                                .serviceName(service.getName())
                                .usageUnits(request.getUsageUnits())
                                .totalCharged(totalFee)
                                .feeBreakdown(feeBreakdownDto)
                                .balanceAfter(newBalance)
                                .availableBalanceAfter(newAvailable)
                                .status(TransactionStatus.SUCCESS.name())
                                .createdAt(now)
                                .referenceId(refId)
                                .metadata(request.getMetadata())
                                .build();

                log.info("========== BILLING PROCESS END ========== refId={}", refId);
                return response;
        }

        /**
         * Calculate fee based on service pricing tiers.
         * First N units charged at initialFee, remaining units charged at
         * subsequentFee.
         */
        private BigDecimal calculateFee(ServicePrice servicePrice, int usageUnits) {
                int initialSize = servicePrice.getInitialSize();
                BigDecimal initialFee = servicePrice.getInitialFee();
                BigDecimal subsequentFee = servicePrice.getSubsequentFee();
                int subsequentSize = servicePrice.getSubsequentSize();

                if (usageUnits <= initialSize) {
                        // Within initial tier
                        return initialFee;
                }

                // Initial fee + subsequent tiers
                int remainingUnits = usageUnits - initialSize;
                int subsequentTiers = (int) Math.ceil((double) remainingUnits / subsequentSize);
                BigDecimal totalSubsequent = subsequentFee.multiply(BigDecimal.valueOf(subsequentTiers));

                return initialFee.add(totalSubsequent);
        }

        private FeeBreakdownStructure buildFeeBreakdown(ServicePrice servicePrice, int usageUnits,
                        BigDecimal totalFee) {
                FeeBreakdownStructure breakdown = new FeeBreakdownStructure();

                if (usageUnits <= servicePrice.getInitialSize()) {
                        breakdown.setStrategy("INITIAL_ONLY");
                        breakdown.setInitialFeeApplied(servicePrice.getInitialFee());
                        breakdown.setSubsequentFeeApplied(BigDecimal.ZERO);
                } else {
                        breakdown.setStrategy("TIERED");
                        breakdown.setInitialFeeApplied(servicePrice.getInitialFee());
                        int remainingUnits = usageUnits - servicePrice.getInitialSize();
                        int subsequentTiers = (int) Math
                                        .ceil((double) remainingUnits / servicePrice.getSubsequentSize());
                        breakdown.setSubsequentFeeApplied(
                                        servicePrice.getSubsequentFee().multiply(BigDecimal.valueOf(subsequentTiers)));
                }

                Map<String, Object> rawDetails = new HashMap<>();
                rawDetails.put("servicePriceId", servicePrice.getId().toString());
                rawDetails.put("initialSize", servicePrice.getInitialSize());
                rawDetails.put("initialFee", servicePrice.getInitialFee());
                rawDetails.put("subsequentSize", servicePrice.getSubsequentSize());
                rawDetails.put("subsequentFee", servicePrice.getSubsequentFee());
                rawDetails.put("usageUnits", usageUnits);
                rawDetails.put("totalFee", totalFee);
                breakdown.setRawCalculationDetails(rawDetails);

                return breakdown;
        }
}
