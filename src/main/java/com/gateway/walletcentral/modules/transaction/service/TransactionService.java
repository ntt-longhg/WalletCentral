package com.gateway.walletcentral.modules.transaction.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorUtil;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.transaction.dto.*;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final MessageProducer messageProducer;

    public TransactionService(TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            MessageProducer messageProducer) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.messageProducer = messageProducer;
    }

    public TransactionResponse create(TransactionCreateRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", request.getWalletId()));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal availableBefore = wallet.getAvailableBalance();
        BigDecimal balanceAfter;
        BigDecimal availableAfter;

        if (request.getType() == TransactionType.DEPOSIT) {
            balanceAfter = balanceBefore.add(request.getAmount());
        } else if (request.getType() == TransactionType.CHARGE) {
            if (availableBefore.compareTo(request.getAmount()) < 0) {
                throw new BusinessException("INSUFFICIENT_BALANCE", "Insufficient available balance");
            }
            balanceAfter = balanceBefore.subtract(request.getAmount());
        } else {
            balanceAfter = balanceBefore.add(request.getAmount());
        }
        availableAfter = balanceAfter.add(wallet.getCreditLimit());

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .type(request.getType())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .availableBalanceBefore(availableBefore)
                .availableBalanceAfter(availableAfter)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription())
                .referenceFrom(request.getReferenceFrom())
                .referenceId(request.getReferenceId())
                .createdAt(OffsetDateTime.now())
                .build();

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        var saved = transactionRepository.save(transaction);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", saved.getId().toString());
        event.put("walletId", wallet.getId().toString());
        event.put("type", request.getType().name());
        event.put("amount", request.getAmount());
        event.put("balanceAfter", balanceAfter);
        messageProducer.publishTransaction(event);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CursorPage<TransactionResponse> list(UUID walletId, TransactionType type, TransactionStatus status,
            CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = transactionRepository.findWithCursor(cursorId, walletId, type, status, pageable)
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
    public TransactionResponse getById(UUID id) {
        var transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public CursorPage<TransactionResponse> listByWallet(UUID walletId, CursorParams params) {
        UUID cursorId = CursorUtil.parseCursor(params.getCursor());
        var pageable = PageRequest.of(0, params.getSize() + 1);

        var items = transactionRepository.findWithCursor(cursorId, walletId, null, null, pageable)
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

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getId())
                .amount(t.getAmount())
                .type(t.getType().name())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .availableBalanceBefore(t.getAvailableBalanceBefore())
                .availableBalanceAfter(t.getAvailableBalanceAfter())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .referenceFrom(t.getReferenceFrom())
                .referenceId(t.getReferenceId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
