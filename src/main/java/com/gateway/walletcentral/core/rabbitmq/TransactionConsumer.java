package com.gateway.walletcentral.core.rabbitmq;

import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.config.RabbitMQConfig;
import com.gateway.walletcentral.modules.notification.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT.TRANSACTION");

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;

    public TransactionConsumer(TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRANSACTION, executor = "virtualThreadExecutor")
    public void handleTransactionCreated(Map<String, Object> message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {
        String transactionId = (String) message.get("transactionId");
        String walletId = (String) message.get("walletId");
        String type = (String) message.get("type");

        log.info("========== TRANSACTION CONSUMER START ==========");
        log.info("TransactionId: {} | WalletId: {} | Type: {}", transactionId, walletId, type);

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
                log.info("Balance verified OK: {} == {} for wallet={}", expectedBalance, actualBalance, wallet.getId());
            }

            // Audit log
            auditLog.info(
                    "TXN_ID={} | WALLET_ID={} | TYPE={} | AMOUNT={} | BALANCE_BEFORE={} | BALANCE_AFTER={} | STATUS={} | REF={} | REF_ID={}",
                    transaction.getId(), wallet.getId(), transaction.getType(),
                    transaction.getAmount(), transaction.getBalanceBefore(), transaction.getBalanceAfter(),
                    transaction.getStatus(), transaction.getReferenceFrom(), transaction.getReferenceId());

            // Large transaction alert → notification
            if (transaction.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
                log.warn("LARGE TRANSACTION ALERT: id={} amount={} wallet={} tenant={}",
                        transaction.getId(), transaction.getAmount(),
                        wallet.getId(), wallet.getTenant().getName());
                try {
                    notificationService.saveAndPush(
                            wallet.getTenant().getId(),
                            "TRANSACTION",
                            "Large Transaction Alert",
                            String.format("Transaction %s: %s VND on wallet %s",
                                    transaction.getId(), transaction.getAmount(), wallet.getId()),
                            "TRANSACTION",
                            transaction.getId().toString());
                } catch (Exception e) {
                    log.error("Failed to send large transaction notification", e);
                }
            }

            // Transaction status verification
            if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                log.warn("Non-success transaction detected: id={} status={}", transaction.getId(),
                        transaction.getStatus());
            }

            log.info("========== TRANSACTION CONSUMER END ========== SUCCESS transaction={}", transactionId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("========== TRANSACTION CONSUMER END ========== FAILED transaction={}", transactionId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
