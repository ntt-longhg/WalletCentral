package com.gateway.walletcentral.modules.usagelog.model;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.servicecatalog.model.Service;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

/**
 * UsageLog entity - records service usage and fee calculation for each tenant.
 * Snapshots wallet financial state at time of usage for audit trail.
 * Contains detailed fee breakdown as JSON.
 */
@Entity
@Table(
    name = "usage_logs",
    comment = "Records service usage and fee calculation for each tenant. Snapshots wallet financial state for audit trail.",
    indexes = {
        @Index(name = "idx_usage_logs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_usage_logs_service_id", columnList = "service_id"),
        @Index(name = "idx_usage_logs_reference", columnList = "reference_from, reference_id"),
        @Index(name = "idx_usage_logs_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Tenant who used the service */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, comment = "Foreign key to tenants.id - tenant who used the service")
    private Tenant tenant;

    /** Service that was used (nullable for wallet plan topup) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = true, comment = "Foreign key to services.id - service that was used (NULL for wallet plan topup)")
    private Service service;

    /** Wallet type snapshot at time of usage (PREPAID/POSTPAID) */
    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type_snapshot", nullable = false, length = 10, comment = "Wallet type snapshot at time of usage: PREPAID, POSTPAID")
    private WalletType walletTypeSnapshot = WalletType.PREPAID;

    /** Total usage units consumed */
    @Column(name = "total_usage", nullable = false, comment = "Total usage units consumed")
    private Integer totalUsage;

    /** Total fee charged for this usage */
    @Column(name = "total_charged", nullable = false, precision = 15, scale = 2, comment = "Total fee charged for this usage")
    private BigDecimal totalCharged = BigDecimal.ZERO;

    /** Credit limit snapshot at time of usage */
    @Column(name = "credit_limit_snapshot", nullable = false, precision = 15, scale = 2, comment = "Credit limit snapshot at time of usage")
    private BigDecimal creditLimitSnapshot = BigDecimal.ZERO;

    /** Available balance (balance + credit_limit) snapshot at time of usage */
    @Column(name = "available_balance_snapshot", nullable = false, precision = 15, scale = 2, comment = "Available balance (balance + credit_limit) snapshot at time of usage")
    private BigDecimal availableBalanceSnapshot = BigDecimal.ZERO;

    /** Detailed fee calculation breakdown as JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fee_breakdown", columnDefinition = "longtext", comment = "Detailed fee calculation breakdown as JSON (strategy, fees, raw details)")
    private FeeBreakdownStructure feeBreakdown;

    /** Source system that initiated this usage record */
    @Column(name = "reference_from", nullable = false, length = 100, comment = "Source system that initiated this usage record (e.g., API, BATCH)")
    private String referenceFrom;

    /** Reference ID from the source system */
    @Column(name = "reference_id", nullable = false, length = 100, comment = "Reference ID from the source system")
    private String referenceId;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
