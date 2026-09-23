package com.gateway.walletcentral.modules.refund.model;

import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "refund_requests",
    comment = "Records tenant refund requests. Tracks lifecycle: PENDING -> APPROVED/REJECTED. Links to original CHARGE transaction.",
    indexes = {
        @Index(name = "idx_refund_requests_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_refund_requests_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_refund_requests_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_refund_requests_status", columnList = "status"),
        @Index(name = "idx_refund_requests_created_at", columnList = "created_at"),
        @Index(name = "idx_refund_requests_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, comment = "Foreign key to transactions.id - original CHARGE transaction being refunded")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, comment = "Foreign key to wallets.id - wallet affected by refund")
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, comment = "Foreign key to tenants.id - tenant requesting refund")
    private Tenant tenant;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, comment = "Refund amount (must match or be less than original transaction amount)")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, comment = "Refund status: PENDING (awaiting approval), APPROVED (refund processed), REJECTED (denied)")
    private RefundRequestStatus status = RefundRequestStatus.PENDING;

    @Column(name = "reason", nullable = true, columnDefinition = "TEXT", comment = "Reason for refund request")
    private String reason;

    @Column(name = "reject_reason", nullable = true, columnDefinition = "TEXT", comment = "Reason for rejection - NULL if not rejected")
    private String rejectReason;

    @Column(name = "requested_by", nullable = false, length = 100, comment = "User who requested the refund")
    private String requestedBy;

    @Column(name = "reviewed_by", nullable = true, length = 100, comment = "Admin who reviewed the refund - NULL if pending")
    private String reviewedBy;

    @Column(name = "reviewed_at", nullable = true, comment = "Timestamp when refund was reviewed - NULL if pending")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", comment = "Record last update timestamp")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at", nullable = true, comment = "Soft delete timestamp - NULL if record is active")
    private OffsetDateTime deletedAt = null;
}
