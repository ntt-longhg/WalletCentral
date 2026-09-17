package com.gateway.walletcentral.modules.invoice.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.invoice.dto.*;
import com.gateway.walletcentral.modules.invoice.model.Invoice;
import com.gateway.walletcentral.modules.invoice.model.InvoiceStatus;
import com.gateway.walletcentral.modules.invoice.repository.InvoiceRepository;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

        private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

        private final InvoiceRepository invoiceRepository;
        private final TenantRepository tenantRepository;
        private final WalletRepository walletRepository;
        private final UsageLogRepository usageLogRepository;

        public InvoiceService(InvoiceRepository invoiceRepository,
                        TenantRepository tenantRepository,
                        WalletRepository walletRepository,
                        UsageLogRepository usageLogRepository) {
                this.invoiceRepository = invoiceRepository;
                this.tenantRepository = tenantRepository;
                this.walletRepository = walletRepository;
                this.usageLogRepository = usageLogRepository;
        }

        public InvoiceResponse create(InvoiceCreateRequest request) {
                if (invoiceRepository.existsByTenantIdAndBillingPeriod(request.getTenantId(),
                                request.getBillingPeriod())) {
                        throw new BusinessException("DUPLICATE_INVOICE", "Invoice already exists for tenant "
                                        + request.getTenantId() + " and period " + request.getBillingPeriod());
                }

                var tenant = tenantRepository.findById(request.getTenantId())
                                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id",
                                                request.getTenantId()));

                var wallet = walletRepository.findById(request.getWalletId())
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id",
                                                request.getWalletId()));

                Invoice invoice = Invoice.builder()
                                .tenant(tenant)
                                .wallet(wallet)
                                .billingPeriod(request.getBillingPeriod())
                                .totalAmount(request.getTotalAmount())
                                .status(InvoiceStatus.ISSUED)
                                .dueDate(request.getDueDate())
                                .createdAt(OffsetDateTime.now())
                                .updatedBy(request.getUpdatedBy())
                                .build();

                var saved = invoiceRepository.save(invoice);
                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public CursorPage<InvoiceResponse> list(UUID tenantId, InvoiceStatus status, CursorParams params) {
                UUID cursorId = CursorUtil.parseCursor(params.getCursor());
                var pageable = PageRequest.of(0, params.getSize() + 1);

                var items = invoiceRepository.findWithCursor(cursorId, tenantId, status, pageable)
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
        public InvoiceResponse getById(UUID id) {
                var invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
                return toResponse(invoice);
        }

        public InvoiceResponse markAsPaid(UUID id, InvoicePayRequest request) {
                var invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

                if (invoice.getStatus() == InvoiceStatus.PAID) {
                        throw new BusinessException("ALREADY_PAID", "Invoice is already paid");
                }

                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setUpdatedBy(request.getUpdatedBy());
                invoice.setUpdatedAt(OffsetDateTime.now());

                var saved = invoiceRepository.save(invoice);
                return toResponse(saved);
        }

        /**
         * Generate invoice for a tenant and billing period.
         * Logic handles prepaid/postpaid wallet type switching:
         * - POSTPAID usage is invoiced (sum total_charged)
         * - PREPAID usage is NOT invoiced (already deducted from balance in real-time)
         * If invoice exists, it updates the total amount.
         */
        public InvoiceResponse generateInvoice(UUID tenantId, String billingPeriod, String updatedBy) {
                var tenant = tenantRepository.findById(tenantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

                var wallet = walletRepository.findByTenantId(tenantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", tenantId));

                YearMonth yearMonth = YearMonth.parse(billingPeriod);
                OffsetDateTime startOfMonth = OffsetDateTime.now().withYear(yearMonth.getYear())
                                .withMonth(yearMonth.getMonthValue())
                                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

                OffsetDateTime endOfMonth = OffsetDateTime.now().withYear(yearMonth.getYear())
                                .withMonth(yearMonth.getMonthValue())
                                .withDayOfMonth(yearMonth.atEndOfMonth().getDayOfMonth()).withHour(23).withMinute(59)
                                .withSecond(59).withNano(0);

                // Sum POSTPAID usage for this period
                BigDecimal postpaidTotal = usageLogRepository.sumChargedByTenantAndPeriod(
                                tenantId, WalletType.POSTPAID, startOfMonth, endOfMonth);

                log.info("Invoice generation for tenant={} period={}: postpaidUsage={}", tenantId, billingPeriod,
                                postpaidTotal);

                // Find existing invoice for this period
                var existingInvoice = invoiceRepository.findByTenantIdAndBillingPeriod(tenantId, billingPeriod);

                if (existingInvoice.isPresent()) {
                        // Update existing invoice
                        var invoice = existingInvoice.get();
                        invoice.setTotalAmount(postpaidTotal);
                        invoice.setWallet(wallet);
                        invoice.setUpdatedBy(updatedBy);
                        invoice.setUpdatedAt(OffsetDateTime.now());
                        var saved = invoiceRepository.save(invoice);
                        log.info("Invoice updated: id={} totalAmount={}", saved.getId(), postpaidTotal);
                        return toResponse(saved);
                } else {
                        // Create new invoice
                        OffsetDateTime dueDate = OffsetDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(23)
                                        .withMinute(59).withSecond(59);

                        Invoice invoice = Invoice.builder()
                                        .tenant(tenant)
                                        .wallet(wallet)
                                        .billingPeriod(billingPeriod)
                                        .totalAmount(postpaidTotal)
                                        .status(InvoiceStatus.ISSUED)
                                        .dueDate(dueDate)
                                        .createdAt(OffsetDateTime.now())
                                        .updatedBy(updatedBy)
                                        .build();

                        var saved = invoiceRepository.save(invoice);
                        log.info("Invoice created: id={} totalAmount={}", saved.getId(), postpaidTotal);
                        return toResponse(saved);
                }
        }

        /**
         * Generate invoices for all active tenants for a given billing period.
         * Only generates invoices for tenants that have POSTPAID wallets (trả sau).
         * Used by scheduler (runs on 1st of each month).
         */
        public int generateAllInvoicesForPeriod(String billingPeriod, String updatedBy) {
                List<UUID> postpaidTenantIds = walletRepository.findTenantIdsByType(WalletType.POSTPAID);

                int count = 0;
                for (UUID tenantId : postpaidTenantIds) {
                        try {
                                generateInvoice(tenantId, billingPeriod, updatedBy);
                                count++;
                        } catch (Exception e) {
                                log.error("Failed to generate invoice for tenant={} period={}: {}",
                                                tenantId, billingPeriod, e.getMessage());
                        }
                }

                log.info("Generated {} invoices for period {} (postpaid tenants only)", count, billingPeriod);
                return count;
        }

        private InvoiceResponse toResponse(Invoice i) {
                return InvoiceResponse.builder()
                                .id(i.getId())
                                .tenantId(i.getTenant().getId())
                                .tenantName(i.getTenant().getName())
                                .walletId(i.getWallet().getId())
                                .billingPeriod(i.getBillingPeriod())
                                .totalAmount(i.getTotalAmount())
                                .status(i.getStatus().name())
                                .dueDate(i.getDueDate())
                                .updatedBy(i.getUpdatedBy())
                                .createdAt(i.getCreatedAt())
                                .updatedAt(i.getUpdatedAt())
                                .build();
        }
}
