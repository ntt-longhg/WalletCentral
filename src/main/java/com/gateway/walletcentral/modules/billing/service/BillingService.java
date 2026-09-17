package com.gateway.walletcentral.modules.billing.service;

import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.billing.dto.BillingWebhookRequest;
import com.gateway.walletcentral.modules.billing.dto.BillingWebhookResponse;
import com.gateway.walletcentral.modules.servicecatalog.model.Service;
import com.gateway.walletcentral.modules.servicecatalog.model.ServicePrice;
import com.gateway.walletcentral.modules.servicecatalog.repository.PriceTierRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServicePriceRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.usagelog.model.FeeBreakdownStructure;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        private final TransactionRepository transactionRepository;
        private final UsageLogRepository usageLogRepository;
        private final MessageProducer messageProducer;

        public BillingService(ServiceRepository serviceRepository,
                        ServicePriceRepository servicePriceRepository,
                        PriceTierRepository priceTierRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        UsageLogRepository usageLogRepository,
                        MessageProducer messageProducer) {
                this.serviceRepository = serviceRepository;
                this.servicePriceRepository = servicePriceRepository;
                this.priceTierRepository = priceTierRepository;
                this.walletRepository = walletRepository;
                this.transactionRepository = transactionRepository;
                this.usageLogRepository = usageLogRepository;
                this.messageProducer = messageProducer;
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
                                .findByServiceIdAndIsActiveTrue(service.getId());
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

                // 8. Create Transaction (CHARGE)
                String refId = UUID.randomUUID().toString();
                Transaction transaction = Transaction.builder()
                                .wallet(wallet)
                                .amount(totalFee)
                                .type(TransactionType.CHARGE)
                                .balanceBefore(balanceBefore)
                                .balanceAfter(newBalance)
                                .availableBalanceBefore(availableBefore)
                                .availableBalanceAfter(newAvailable)
                                .status(TransactionStatus.SUCCESS)
                                .description(request.getDescription() != null ? request.getDescription()
                                                : "Thanh toán cước" + service.getCode())
                                .referenceFrom("BILLING_WEBHOOK")
                                .referenceId(refId)
                                .createdAt(OffsetDateTime.now())
                                .build();
                transactionRepository.save(transaction);
                log.info("Transaction created: id={} type=CHARGE amount={}", transaction.getId(), totalFee);

                // Publish transaction.created event for TransactionConsumer
                Map<String, Object> txnEvent = new HashMap<>();
                txnEvent.put("transactionId", transaction.getId().toString());
                txnEvent.put("walletId", wallet.getId().toString());
                txnEvent.put("type", TransactionType.CHARGE.name());
                txnEvent.put("amount", totalFee);
                txnEvent.put("balanceAfter", newBalance);
                messageProducer.publishTransaction(txnEvent);

                // 9. Create UsageLog
                FeeBreakdownStructure feeBreakdownStructure = buildFeeBreakdown(servicePrice, request.getUsageUnits(),
                                totalFee);
                UsageLog usageLog = UsageLog.builder()
                                .tenant(tenant)
                                .service(service)
                                .walletTypeSnapshot(wallet.getType())
                                .totalUsage(request.getUsageUnits())
                                .totalCharged(totalFee)
                                .creditLimitSnapshot(wallet.getCreditLimit())
                                .availableBalanceSnapshot(newAvailable)
                                .feeBreakdown(feeBreakdownStructure)
                                .referenceFrom("BILLING_WEBHOOK")
                                .referenceId(refId)
                                .createdAt(OffsetDateTime.now())
                                .build();
                usageLogRepository.save(usageLog);
                log.info("UsageLog created: id={} totalCharged={}", usageLog.getId(), totalFee);

                // Publish usage.log.recorded event for UsageLogConsumer
                Map<String, Object> usageEvent = new HashMap<>();
                usageEvent.put("usageLogId", usageLog.getId().toString());
                usageEvent.put("tenantId", tenant.getId().toString());
                usageEvent.put("serviceId", service.getId().toString());
                messageProducer.publishUsageLog(usageEvent);

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
                                .transactionId(transaction.getId().toString())
                                .tenantId(tenant.getId().toString())
                                .serviceCode(service.getCode())
                                .serviceName(service.getName())
                                .usageUnits(request.getUsageUnits())
                                .totalCharged(totalFee)
                                .feeBreakdown(feeBreakdownDto)
                                .balanceAfter(newBalance)
                                .availableBalanceAfter(newAvailable)
                                .status(TransactionStatus.SUCCESS.name())
                                .createdAt(transaction.getCreatedAt())
                                .referenceId(refId)
                                .metadata(request.getMetadata())
                                .build();

                // 11. Publish event for async webhook callback
                Map<String, Object> event = new HashMap<>();
                event.put("transactionId", transaction.getId().toString());
                event.put("usageLogId", usageLog.getId().toString());
                event.put("webhookUrl", request.getWebhookUrl());
                event.put("webhookAuth", request.getWebhookAuth());
                event.put("response", response);
                messageProducer.publishBilling(event);

                log.info("========== BILLING PROCESS END ========== transaction={}", transaction.getId());
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
