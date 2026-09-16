package com.gateway.walletcentral.modules.wallet.model;

import com.gateway.walletcentral.modules.tenant.model.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Wallet entity - manages balance and credit limit for each tenant.
 * Supports two types: PREPAID (balance-based) and POSTPAID (credit-based).
 * Available balance = balance + credit_limit.
 */
@Entity
@Table(name = "wallets", comment = "Manages financial wallet for each tenant. Tracks balance and credit limit. Available balance = balance + credit_limit.", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallets_tenant_id", columnNames = "tenant_id")
}, indexes = {
        @Index(name = "idx_wallets_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_wallets_status", columnList = "status"),
        @Index(name = "idx_wallets_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Associated tenant - one-to-one relationship */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true, comment = "Foreign key to tenants.id - one wallet per tenant")
    private Tenant tenant;

    /** Wallet type: PREPAID (balance-based) or POSTPAID (credit-based) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10, comment = "Wallet type: PREPAID (balance-based), POSTPAID (credit-based)")
    private WalletType type = WalletType.PREPAID;

    /** Current balance amount - deducted when services are used */
    @Column(name = "balance", nullable = false, precision = 15, scale = 2, comment = "Current balance amount - deducted when services are used")
    private BigDecimal balance = BigDecimal.ZERO;

    /** Credit limit - maximum allowable negative balance for POSTPAID wallets */
    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2, comment = "Credit limit - maximum allowable negative balance for POSTPAID wallets")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    /** Wallet status: ACTIVE, SUSPENDED, CLOSED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, comment = "Wallet status: ACTIVE, SUSPENDED, CLOSED")
    private WalletStatus status = WalletStatus.ACTIVE;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /** Record last update timestamp */
    @Column(name = "updated_at", comment = "Record last update timestamp")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /** Soft delete timestamp - NULL if not deleted */
    @Column(name = "deleted_at", nullable = true, comment = "Soft delete timestamp - NULL if record is active")
    private OffsetDateTime deletedAt = null;

    /**
     * Calculate available balance = balance + credit_limit.
     * Used to check if tenant can afford a charge.
     */
    @Transient
    public BigDecimal getAvailableBalance() {
        return balance.add(creditLimit);
    }
}
