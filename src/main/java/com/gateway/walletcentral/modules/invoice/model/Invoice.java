package com.gateway.walletcentral.modules.invoice.model;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Invoice entity - represents a billing invoice for a tenant.
 * Tracks total amount due and payment status for each billing period.
 */
@Entity
@Table(
    name = "invoices",
    comment = "Represents a billing invoice for a tenant. Tracks total amount due and payment status per billing period.",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoices_tenant_period", columnNames = {"tenant_id", "billing_period"})
    },
    indexes = {
        @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoices_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_invoices_status", columnList = "status"),
        @Index(name = "idx_invoices_billing_period", columnList = "billing_period"),
        @Index(name = "idx_invoices_due_date", columnList = "due_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Tenant who owns this invoice */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, comment = "Foreign key to tenants.id - tenant who owns this invoice")
    private Tenant tenant;

    /** Wallet associated with this invoice */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, comment = "Foreign key to wallets.id - wallet associated with this invoice")
    private Wallet wallet;

    /** Billing period in yyyy-MM format */
    @Column(name = "billing_period", nullable = false, length = 7, comment = "Billing period in yyyy-MM format (e.g., 2026-01)")
    private String billingPeriod;

    /** Total amount due on this invoice */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2, comment = "Total amount due on this invoice")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Invoice status: ISSUED, PAID */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, comment = "Invoice status: ISSUED (awaiting payment), PAID (payment received)")
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    /** Payment due date */
    @Column(name = "due_date", updatable = false, comment = "Payment due date")
    private LocalDateTime dueDate = LocalDateTime.now();

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Record last update timestamp */
    @Column(name = "updated_at", comment = "Record last update timestamp")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** User who last updated this record */
    @Column(name = "updated_by", nullable = false, length = 100, comment = "User who last updated this record")
    private String updatedBy;
}
