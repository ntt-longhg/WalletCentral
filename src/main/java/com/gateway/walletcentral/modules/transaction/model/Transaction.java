package com.gateway.walletcentral.modules.transaction.model;

import com.gateway.walletcentral.modules.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Transaction entity - records all financial movements on a wallet.
 * Tracks balance changes and available balance snapshots for audit trail.
 */
@Entity
@Table(
    name = "transactions",
    comment = "Records all financial transactions (deposits, charges, refunds) on a wallet. Provides full audit trail with balance snapshots.",
    indexes = {
        @Index(name = "idx_transactions_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_transactions_type", columnList = "type"),
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at"),
        @Index(name = "idx_transactions_reference", columnList = "reference_from, reference_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Associated wallet */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, comment = "Foreign key to wallets.id - wallet this transaction belongs to")
    private Wallet wallet;

    /** Transaction amount - always positive (DEPOSIT adds, CHARGE deducts from balance) */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2, comment = "Transaction amount - always positive value")
    private BigDecimal amount = BigDecimal.ZERO;

    /** Transaction type: DEPOSIT (add funds), CHARGE (deduct funds), REFUND (return funds) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10, comment = "Transaction type: DEPOSIT (add funds), CHARGE (deduct funds), REFUND (return funds)")
    private TransactionType type = TransactionType.CHARGE;

    /** Balance before this transaction */
    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2, comment = "Wallet balance before this transaction")
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    /** Balance after this transaction */
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2, comment = "Wallet balance after this transaction")
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    /** Available balance (balance + credit_limit) before this transaction */
    @Column(name = "available_balance_before", nullable = false, precision = 15, scale = 2, comment = "Available balance (balance + credit_limit) before this transaction")
    private BigDecimal availableBalanceBefore = BigDecimal.ZERO;

    /** Available balance (balance + credit_limit) after this transaction */
    @Column(name = "available_balance_after", nullable = false, precision = 15, scale = 2, comment = "Available balance (balance + credit_limit) after this transaction")
    private BigDecimal availableBalanceAfter = BigDecimal.ZERO;

    /** Transaction status: SUCCESS, FAILED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, comment = "Transaction status: SUCCESS, FAILED")
    private TransactionStatus status = TransactionStatus.SUCCESS;

    /** Optional description or note for this transaction */
    @Column(name = "description", nullable = true, columnDefinition = "TEXT", comment = "Optional description or note for this transaction")
    private String description = null;

    /** Source system that initiated this transaction (e.g., ADMIN, SYSTEM, PAYMENT_GATEWAY) */
    @Column(name = "reference_from", nullable = false, length = 100, comment = "Source system that initiated this transaction (e.g., ADMIN, SYSTEM, PAYMENT_GATEWAY)")
    private String referenceFrom;

    /** Reference ID from the source system */
    @Column(name = "reference_id", nullable = false, length = 100, comment = "Reference ID from the source system")
    private String referenceId;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
