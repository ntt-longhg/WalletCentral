package com.gateway.walletcentral.modules.invoice.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.invoice.dto.*;
import com.gateway.walletcentral.modules.invoice.model.Invoice;
import com.gateway.walletcentral.modules.invoice.model.InvoiceStatus;
import com.gateway.walletcentral.modules.invoice.repository.InvoiceRepository;
import com.gateway.walletcentral.modules.refund.model.RefundRequestStatus;
import com.gateway.walletcentral.modules.refund.repository.RefundRequestRepository;
import com.gateway.walletcentral.modules.systemconfig.service.SystemConfigService;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.usagelog.repository.UsageLogRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        private final RefundRequestRepository refundRequestRepository;
        private final TransactionRepository transactionRepository;
        private final SystemConfigService configService;
        private final ApplicationEventPublisher eventPublisher;

        public InvoiceService(InvoiceRepository invoiceRepository,
                        TenantRepository tenantRepository,
                        WalletRepository walletRepository,
                        UsageLogRepository usageLogRepository,
                        RefundRequestRepository refundRequestRepository,
                        TransactionRepository transactionRepository,
                        SystemConfigService configService,
                        ApplicationEventPublisher eventPublisher) {
                this.invoiceRepository = invoiceRepository;
                this.tenantRepository = tenantRepository;
                this.walletRepository = walletRepository;
                this.usageLogRepository = usageLogRepository;
                this.refundRequestRepository = refundRequestRepository;
                this.transactionRepository = transactionRepository;
                this.configService = configService;
                this.eventPublisher = eventPublisher;
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
                                .createdAt(LocalDateTime.now())
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

                // Paying an invoice tops the POSTPAID wallet back up: billing deducted
                // the usage from balance when charging, so payment credits it back.
                // Zero-amount invoices move no money.
                if (invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                        var wallet = invoice.getWallet();
                        BigDecimal balanceBefore = wallet.getBalance();
                        BigDecimal availableBefore = wallet.getAvailableBalance();
                        BigDecimal newBalance = balanceBefore.add(invoice.getTotalAmount());
                        BigDecimal newAvailable = newBalance.add(wallet.getCreditLimit());

                        wallet.setBalance(newBalance);
                        wallet.setUpdatedAt(LocalDateTime.now());
                        walletRepository.save(wallet);

                        Transaction paymentTxn = Transaction.builder()
                                        .wallet(wallet)
                                        .amount(invoice.getTotalAmount())
                                        .type(TransactionType.DEPOSIT)
                                        .balanceBefore(balanceBefore)
                                        .balanceAfter(newBalance)
                                        .availableBalanceBefore(availableBefore)
                                        .availableBalanceAfter(newAvailable)
                                        .status(TransactionStatus.SUCCESS)
                                        .description("Thanh toán hóa đơn kỳ " + invoice.getBillingPeriod())
                                        .referenceFrom("INVOICE")
                                        .referenceId(invoice.getId().toString())
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        transactionRepository.save(paymentTxn);
                        log.info("Invoice paid: id={} amount={} wallet {} balance {} -> {}",
                                        id, invoice.getTotalAmount(), wallet.getId(), balanceBefore, newBalance);
                }

                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setUpdatedBy(request.getUpdatedBy());
                invoice.setUpdatedAt(LocalDateTime.now());

                var saved = invoiceRepository.save(invoice);

                NotificationEvent notificationEvent = NotificationEvent.builder()
                                .tenantId(saved.getTenant().getId().toString())
                                .type("INVOICE")
                                .title("Hóa đơn đã được thanh toán")
                                .message(String.format("Hóa đơn kỳ %s với số tiền %s VND đã được thanh toán.",
                                                saved.getBillingPeriod(), saved.getTotalAmount()))
                                .referenceType("INVOICE")
                                .referenceId(saved.getId().toString())
                                .build();
                eventPublisher.publishEvent(notificationEvent);

                return toResponse(saved);
        }

        /**
         * Generate invoice for a tenant and billing period.
         * Logic handles prepaid/postpaid wallet type switching:
         * - POSTPAID usage is invoiced (sum total_charged)
         * - PREPAID usage is NOT invoiced (already deducted from balance in real-time)
         * - Usage whose charge was refunded (APPROVED refund) is excluded
         * - Zero-amount invoices are auto-PAID with a note; if a later
         * regeneration finds new usage, note/status revert to ISSUED
         * If invoice exists, it updates the total amount.
         */
        public InvoiceResponse generateInvoice(UUID tenantId, String billingPeriod, String updatedBy) {
                var tenant = tenantRepository.findById(tenantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

                var wallet = walletRepository.findByTenantId(tenantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", tenantId));

                LocalDateTime[] bounds = periodBounds(billingPeriod);
                LocalDateTime startOfMonth = bounds[0];
                LocalDateTime endOfMonth = bounds[1];

                // Sum POSTPAID usage for this period, excluding refunded charges
                List<String> refundedRefs = refundRequestRepository
                                .findTransactionReferenceIdsByTenantAndStatus(tenantId, RefundRequestStatus.APPROVED);
                BigDecimal postpaidTotal;
                if (refundedRefs.isEmpty()) {
                        postpaidTotal = usageLogRepository.sumChargedByTenantAndPeriod(
                                        tenantId, WalletType.POSTPAID, startOfMonth, endOfMonth);
                } else {
                        postpaidTotal = usageLogRepository.sumChargedByTenantAndPeriodExcluding(
                                        tenantId, WalletType.POSTPAID, startOfMonth, endOfMonth, refundedRefs);
                }

                log.info("Invoice generation for tenant={} period={}: postpaidUsage={} ({} refunded refs excluded)",
                                tenantId, billingPeriod, postpaidTotal, refundedRefs.size());

                // Find existing invoice for this period
                var existingInvoice = invoiceRepository.findByTenantIdAndBillingPeriod(tenantId, billingPeriod);

                if (existingInvoice.isPresent()) {
                        // Update existing invoice
                        var invoice = existingInvoice.get();
                        invoice.setTotalAmount(postpaidTotal);
                        invoice.setWallet(wallet);
                        invoice.setUpdatedBy(updatedBy);
                        invoice.setUpdatedAt(LocalDateTime.now());
                        applyZeroAmountRule(invoice, postpaidTotal);
                        var saved = invoiceRepository.save(invoice);
                        log.info("Invoice updated: id={} totalAmount={} status={}", saved.getId(), postpaidTotal,
                                        saved.getStatus());
                        return toResponse(saved);
                } else {
                        // Create new invoice (due date is dynamic via system_config)
                        LocalDateTime dueDate = calculateDueDate();

                        Invoice invoice = Invoice.builder()
                                        .tenant(tenant)
                                        .wallet(wallet)
                                        .billingPeriod(billingPeriod)
                                        .totalAmount(postpaidTotal)
                                        .status(InvoiceStatus.ISSUED)
                                        .dueDate(dueDate)
                                        .createdAt(LocalDateTime.now())
                                        .updatedBy(updatedBy)
                                        .build();
                        applyZeroAmountRule(invoice, postpaidTotal);

                        var saved = invoiceRepository.save(invoice);
                        log.info("Invoice created: id={} totalAmount={} status={}", saved.getId(), postpaidTotal,
                                        saved.getStatus());
                        return toResponse(saved);
                }
        }

        /**
         * Zero-amount rule: no usage in the period means nothing to pay, so the
         * invoice is PAID immediately with an explanatory note. If a later
         * regeneration finds new usage, an auto-paid invoice reverts to ISSUED
         * with the note cleared. Manually paid invoices are never reopened.
         */
        private void applyZeroAmountRule(Invoice invoice, BigDecimal total) {
                String zeroNote = configService.getValue("invoice.zero_amount_note",
                                "Không có phát sinh giao dịch trong tháng");
                if (total.compareTo(BigDecimal.ZERO) == 0) {
                        invoice.setStatus(InvoiceStatus.PAID);
                        invoice.setNote(zeroNote);
                } else if (zeroNote.equals(invoice.getNote())) {
                        invoice.setNote(null);
                        invoice.setStatus(InvoiceStatus.ISSUED);
                }
        }

        /**
         * Billable lines of an invoice: POSTPAID usage in the period, each
         * linked to its charge transaction and flagged when refunded.
         * Cursor-paginated (default size 20 like other list endpoints).
         */
        @Transactional(readOnly = true)
        public CursorPage<InvoiceItemResponse> getInvoiceItems(UUID invoiceId, CursorParams params) {
                var invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
                return buildInvoiceItemsPaged(invoice.getTenant().getId(), invoice.getBillingPeriod(), params);
        }

        @Transactional(readOnly = true)
        public CursorPage<InvoiceItemResponse> getInvoiceItemsForTenant(UUID invoiceId, UUID tenantId,
                        CursorParams params) {
                var invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
                if (!invoice.getTenant().getId().equals(tenantId)) {
                        throw new BusinessException("FORBIDDEN", "Invoice does not belong to this tenant");
                }
                return buildInvoiceItemsPaged(tenantId, invoice.getBillingPeriod(), params);
        }

        private CursorPage<InvoiceItemResponse> buildInvoiceItemsPaged(UUID tenantId, String billingPeriod,
                        CursorParams params) {
                UUID cursorId = CursorUtil.parseCursor(params.getCursor());
                var pageable = PageRequest.of(0, params.getSize() + 1);

                var items = buildInvoiceItems(tenantId, billingPeriod, cursorId, pageable);

                boolean hasNext = items.size() > params.getSize();
                if (hasNext) {
                        items = items.subList(0, params.getSize());
                }
                String nextCursor = hasNext && !items.isEmpty()
                                ? items.get(items.size() - 1).getUsageLogId().toString()
                                : null;
                return CursorPage.of(items, nextCursor, hasNext, params.getSize());
        }

        private List<InvoiceItemResponse> buildInvoiceItems(UUID tenantId, String billingPeriod, UUID cursorId,
                        org.springframework.data.domain.Pageable pageable) {
                LocalDateTime[] bounds = periodBounds(billingPeriod);

                var usageLogs = (pageable == null)
                                ? usageLogRepository.findByTenantAndPeriod(
                                                tenantId, WalletType.POSTPAID, bounds[0], bounds[1], null,
                                                org.springframework.data.domain.Pageable.unpaged())
                                : usageLogRepository.findByTenantAndPeriod(
                                                tenantId, WalletType.POSTPAID, bounds[0], bounds[1], cursorId,
                                                pageable);

                var refundedRefs = new java.util.HashSet<>(refundRequestRepository
                                .findTransactionReferenceIdsByTenantAndStatus(tenantId, RefundRequestStatus.APPROVED));

                var refIds = usageLogs.stream()
                                .map(UsageLog::getReferenceId)
                                .filter(id -> id != null)
                                .distinct()
                                .toList();
                var txnByRef = new java.util.HashMap<String, Transaction>();
                if (!refIds.isEmpty()) {
                        for (Transaction txn : transactionRepository.findByReferenceIdIn(refIds)) {
                                txnByRef.putIfAbsent(txn.getReferenceId(), txn);
                        }
                }

                return usageLogs.stream()
                                .map(ul -> {
                                        boolean refunded = ul.getReferenceId() != null
                                                        && refundedRefs.contains(ul.getReferenceId());
                                        Transaction txn = ul.getReferenceId() != null
                                                        ? txnByRef.get(ul.getReferenceId())
                                                        : null;
                                        return InvoiceItemResponse.builder()
                                                        .usageLogId(ul.getId())
                                                        .serviceName(ul.getService() != null
                                                                        ? ul.getService().getName()
                                                                        : null)
                                                        .serviceCode(ul.getService() != null
                                                                        ? ul.getService().getCode()
                                                                        : null)
                                                        .totalUsage(ul.getTotalUsage())
                                                        .totalCharged(ul.getTotalCharged())
                                                        .referenceId(ul.getReferenceId())
                                                        .refunded(refunded)
                                                        .transactionId(txn != null ? txn.getId() : null)
                                                        .transactionStatus(txn != null
                                                                        ? txn.getStatus().name()
                                                                        : null)
                                                        .createdAt(ul.getCreatedAt())
                                                        .build();
                                })
                                .toList();
        }

        private LocalDateTime[] periodBounds(String billingPeriod) {
                YearMonth yearMonth = YearMonth.parse(billingPeriod);
                LocalDateTime startOfMonth = LocalDateTime.now().withYear(yearMonth.getYear())
                                .withMonth(yearMonth.getMonthValue())
                                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

                LocalDateTime endOfMonth = LocalDateTime.now().withYear(yearMonth.getYear())
                                .withMonth(yearMonth.getMonthValue())
                                .withDayOfMonth(yearMonth.atEndOfMonth().getDayOfMonth()).withHour(23).withMinute(59)
                                .withSecond(59).withNano(0);
                return new LocalDateTime[] { startOfMonth, endOfMonth };
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
                                .note(i.getNote())
                                .dueDate(i.getDueDate())
                                .updatedBy(i.getUpdatedBy())
                                .createdAt(i.getCreatedAt())
                                .updatedAt(i.getUpdatedAt())
                                .build();
        }

        /**
         * Due date from system_config (invoice.due_month_offset / due_day / due_time).
         * Day is clamped to the target month length so short months never fail.
         */
        private LocalDateTime calculateDueDate() {
                int monthOffset = configService.getInt("invoice.due_month_offset", 1);
                int dueDay = configService.getInt("invoice.due_day", 1);
                LocalTime dueTime = parseDueTime(configService.getValue("invoice.due_time", "23:59:59"));

                LocalDateTime base = LocalDateTime.now().plusMonths(monthOffset);
                int maxDay = YearMonth.of(base.getYear(), base.getMonthValue()).lengthOfMonth();
                int day = Math.min(Math.max(dueDay, 1), maxDay);
                return base.withDayOfMonth(day)
                                .withHour(dueTime.getHour())
                                .withMinute(dueTime.getMinute())
                                .withSecond(dueTime.getSecond())
                                .withNano(0);
        }

        private LocalTime parseDueTime(String value) {
                try {
                        return LocalTime.parse(value.trim());
                } catch (Exception e) {
                        log.warn("Invalid invoice.due_time '{}', using 23:59:59", value);
                        return LocalTime.of(23, 59, 59);
                }
        }
}
