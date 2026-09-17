package com.gateway.walletcentral.modules.transaction.handler;

import com.gateway.walletcentral.core.rabbitmq.MessageProducer;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventHandler.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.TRANSACTION");

    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000000");

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final MessageProducer messageProducer;

    public TransactionEventHandler(TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            MessageProducer messageProducer) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.messageProducer = messageProducer;
    }

    public void handleTransactionCreated(Map<String, Object> payload, Channel channel, long deliveryTag)
            throws IOException {
        String transactionId = (String) payload.get("transactionId");
        String walletId = (String) payload.get("walletId");
        log.info("Transaction handler - transactionId: {} | walletId: {} | thread: {}", transactionId, walletId,
                Thread.currentThread());

        try {
            Transaction transaction = transactionRepository.findById(UUID.fromString(transactionId)).orElse(null);
            if (transaction == null) {
                log.error("Transaction not found: {} - possible data inconsistency", transactionId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Wallet wallet = transaction.getWallet();

            // Balance reconciliation
            BigDecimal expectedBalance = transaction.getBalanceAfter();
            BigDecimal actualBalance = wallet.getBalance();
            if (expectedBalance.compareTo(actualBalance) != 0) {
                log.warn("BALANCE MISMATCH! TransactionId: {} | Expected: {} | Actual: {} | WalletId: {}",
                        transactionId, expectedBalance, actualBalance, wallet.getId());
                wallet.setBalance(expectedBalance);
                walletRepository.save(wallet);
                log.info("Auto-rebalanced wallet {} to {}", wallet.getId(), expectedBalance);
            } else {
                log.info("Balance verified OK: {} == {} for wallet={}", expectedBalance, actualBalance,
                        wallet.getId());
            }

            // Audit log
            auditLog.info(
                    "TXN_ID={} | WALLET_ID={} | TYPE={} | AMOUNT={} | BALANCE_BEFORE={} | BALANCE_AFTER={} | STATUS={} | REF={} | REF_ID={}",
                    transaction.getId(), wallet.getId(), transaction.getType(),
                    transaction.getAmount(), transaction.getBalanceBefore(), transaction.getBalanceAfter(),
                    transaction.getStatus(), transaction.getReferenceFrom(), transaction.getReferenceId());

            // Large transaction alert
            if (transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) > 0) {
                log.warn("LARGE TRANSACTION ALERT: id={} amount={} wallet={} tenant={}",
                        transaction.getId(), transaction.getAmount(),
                        wallet.getId(), wallet.getTenant().getName());
                Map<String, Object> notificationEvent = new HashMap<>();
                notificationEvent.put("tenantId", wallet.getTenant().getId().toString());
                notificationEvent.put("type", "TRANSACTION");
                notificationEvent.put("title", "Cảnh báo giao dịch lớn");
                notificationEvent.put("message", String.format("Giao dịch %s với số tiền %s VND trên ví %s",
                        transaction.getId(), transaction.getAmount(), wallet.getId()));
                notificationEvent.put("referenceType", "TRANSACTION");
                notificationEvent.put("referenceId", transaction.getId().toString());
                messageProducer.publishNotification(notificationEvent);
            }

            // Transaction status verification
            if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                log.warn("Non-success transaction detected: id={} status={}", transaction.getId(),
                        transaction.getStatus());
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Transaction handler error: transactionId={}", transactionId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
