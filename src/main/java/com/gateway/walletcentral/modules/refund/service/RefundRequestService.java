package com.gateway.walletcentral.modules.refund.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.event.NotificationEvent;
import com.gateway.walletcentral.core.event.RefundApprovedEvent;
import com.gateway.walletcentral.core.event.RefundRejectedEvent;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.refund.dto.*;
import com.gateway.walletcentral.modules.refund.model.RefundRequest;
import com.gateway.walletcentral.modules.refund.model.RefundRequestStatus;
import com.gateway.walletcentral.modules.refund.repository.RefundRequestRepository;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RefundRequestService {

    private static final Logger log = LoggerFactory.getLogger(RefundRequestService.class);

    private final RefundRequestRepository refundRequestRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RefundRequestService(RefundRequestRepository refundRequestRepository,
                                TransactionRepository transactionRepository,
                                WalletRepository walletRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.refundRequestRepository = refundRequestRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }

    public RefundResponse createRefundRequest(RefundCreateRequest request) {
        log.info("Creating refund request for transactionId: {}", request.getTransactionId());

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", request.getTransactionId()));

        if (transaction.getType() != TransactionType.CHARGE) {
            throw new BusinessException("INVALID_TRANSACTION_TYPE",
                    "Only CHARGE transactions can be refunded. Found: " + transaction.getType());
        }
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new BusinessException("INVALID_TRANSACTION_STATUS",
                    "Only SUCCESS transactions can be refunded. Found: " + transaction.getStatus());
        }

        List<RefundRequestStatus> activeStatuses = List.of(RefundRequestStatus.PENDING, RefundRequestStatus.APPROVED);
        if (refundRequestRepository.existsByTransactionIdAndStatusIn(transaction.getId(), activeStatuses)) {
            throw new BusinessException("REFUND_ALREADY_EXISTS",
                    "A pending or approved refund request already exists for this transaction");
        }

        Wallet wallet = transaction.getWallet();

        RefundRequest refundRequest = RefundRequest.builder()
                .transaction(transaction)
                .wallet(wallet)
                .tenant(wallet.getTenant())
                .amount(transaction.getAmount())
                .status(RefundRequestStatus.PENDING)
                .reason(request.getReason())
                .requestedBy(request.getRequestedBy() != null ? request.getRequestedBy() : "system")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var saved = refundRequestRepository.save(refundRequest);
        log.info("Refund request created: id={} transactionId={} amount={}", saved.getId(), transaction.getId(), saved.getAmount());

        return toResponse(saved);
    }

    @Transactional
    public RefundResponse approveRefundRequest(UUID id, RefundApproveRequest request) {
        log.info("Approving refund request: {}", id);

        RefundRequest refundRequest = refundRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundRequest", "id", id));

        if (refundRequest.getStatus() != RefundRequestStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Refund request must be PENDING to approve");
        }

        Wallet wallet = walletRepository.findByTenantIdForUpdate(refundRequest.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "tenantId", refundRequest.getTenant().getId()));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal availableBefore = wallet.getAvailableBalance();
        BigDecimal newBalance = balanceBefore.add(refundRequest.getAmount());
        BigDecimal newAvailable = newBalance.add(wallet.getCreditLimit());

        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setReviewedBy(request.getReviewedBy());
        refundRequest.setReviewedAt(LocalDateTime.now());
        refundRequest.setUpdatedAt(LocalDateTime.now());
        var saved = refundRequestRepository.save(refundRequest);

        RefundApprovedEvent refundEvent = RefundApprovedEvent.builder()
                .refundRequestId(saved.getId().toString())
                .transactionId(saved.getTransaction().getId().toString())
                .walletId(wallet.getId().toString())
                .tenantId(wallet.getTenant().getId().toString())
                .amount(saved.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .availableBalanceBefore(availableBefore)
                .availableBalanceAfter(newAvailable)
                .reviewedBy(request.getReviewedBy())
                .build();
        eventPublisher.publishEvent(refundEvent);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .tenantId(wallet.getTenant().getId().toString())
                .type("REFUND")
                .title("Yêu cầu hoàn tiền được duyệt")
                .message(String.format("Yêu cầu hoàn tiền %s đã được duyệt. Số tiền %s VND đã được cộng vào ví.",
                        saved.getId(), saved.getAmount()))
                .referenceType("REFUND_REQUEST")
                .referenceId(saved.getId().toString())
                .build();
        eventPublisher.publishEvent(notificationEvent);

        log.info("Refund request approved: id={} amount={}", id, saved.getAmount());
        return toResponse(saved);
    }

    @Transactional
    public RefundResponse rejectRefundRequest(UUID id, RefundRejectRequest request) {
        log.info("Rejecting refund request: {}", id);

        RefundRequest refundRequest = refundRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundRequest", "id", id));

        if (refundRequest.getStatus() != RefundRequestStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Refund request must be PENDING to reject");
        }

        refundRequest.setStatus(RefundRequestStatus.REJECTED);
        refundRequest.setReviewedBy(request.getReviewedBy());
        refundRequest.setReviewedAt(LocalDateTime.now());
        refundRequest.setRejectReason(request.getRejectReason());
        refundRequest.setUpdatedAt(LocalDateTime.now());
        var saved = refundRequestRepository.save(refundRequest);

        // Record a FAILED transaction in the ledger so the tenant can see
        // why the refund never happened. Balances are unchanged (no money moved).
        Wallet wallet = saved.getWallet();
        Transaction failedTxn = Transaction.builder()
                .wallet(wallet)
                .amount(saved.getAmount())
                .type(TransactionType.REFUND)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance())
                .availableBalanceBefore(wallet.getAvailableBalance())
                .availableBalanceAfter(wallet.getAvailableBalance())
                .status(TransactionStatus.FAILED)
                .description("Yêu cầu hoàn tiền cho giao dịch " + saved.getTransaction().getId()
                        + " bị từ chối: " + request.getRejectReason())
                .referenceFrom("REFUND")
                .referenceId(saved.getId().toString())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(failedTxn);
        log.info("Recorded FAILED transaction {} for rejected refund request {}", failedTxn.getId(), id);

        RefundRejectedEvent refundEvent = RefundRejectedEvent.builder()
                .refundRequestId(saved.getId().toString())
                .tenantId(saved.getTenant().getId().toString())
                .reviewedBy(request.getReviewedBy())
                .rejectReason(request.getRejectReason())
                .build();
        eventPublisher.publishEvent(refundEvent);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .tenantId(saved.getTenant().getId().toString())
                .type("REFUND")
                .title("Yêu cầu hoàn tiền bị từ chối")
                .message(String.format("Yêu cầu hoàn tiền %s đã bị từ chối. Lý do: %s",
                        saved.getId(), request.getRejectReason()))
                .referenceType("REFUND_REQUEST")
                .referenceId(saved.getId().toString())
                .build();
        eventPublisher.publishEvent(notificationEvent);

        log.info("Refund request rejected: id={}", id);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<RefundResponse> list(UUID tenantId, RefundRequestStatus status, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = refundRequestRepository.findWithCursor(cursorId, tenantId, status, pageable)
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
    public RefundResponse getById(UUID id) {
        var refundRequest = refundRequestRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundRequest", "id", id));
        return toResponse(refundRequest);
    }

    @Transactional(readOnly = true)
    public CursorPage<RefundResponse> listPending(CursorParams params) {
        var items = refundRequestRepository.findByStatusOrderByCreatedAtAsc(RefundRequestStatus.PENDING)
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
    public CursorPage<RefundResponse> listPendingByTenant(UUID tenantId, CursorParams params) {
        var items = refundRequestRepository.findByTenantIdAndStatusOrderByCreatedAtAsc(tenantId, RefundRequestStatus.PENDING)
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

    private RefundResponse toResponse(RefundRequest rr) {
        return RefundResponse.builder()
                .id(rr.getId())
                .transactionId(rr.getTransaction().getId())
                .transactionAmount(rr.getTransaction().getAmount())
                .transactionType(rr.getTransaction().getType().name())
                .transactionStatus(rr.getTransaction().getStatus().name())
                .walletId(rr.getWallet().getId())
                .tenantId(rr.getTenant().getId())
                .tenantName(rr.getTenant().getName())
                .amount(rr.getAmount())
                .status(rr.getStatus().name())
                .reason(rr.getReason())
                .rejectReason(rr.getRejectReason())
                .requestedBy(rr.getRequestedBy())
                .reviewedBy(rr.getReviewedBy())
                .reviewedAt(rr.getReviewedAt())
                .createdAt(rr.getCreatedAt())
                .updatedAt(rr.getUpdatedAt())
                .build();
    }
}
