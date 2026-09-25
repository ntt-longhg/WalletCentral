package com.gateway.walletcentral.modules.creditadjustment.model;

import com.gateway.walletcentral.modules.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * CreditAdjustment entity - audit trail for all credit limit changes.
 * Records every increase, decrease, or set operation on a wallet's credit limit.
 */
@Entity
@Table(
    name = "credit_adjustments",
    comment = "Audit trail for all credit limit changes. Records every increase, decrease, or set operation on a wallet's credit limit.",
    indexes = {
        @Index(name = "idx_credit_adjustments_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_credit_adjustments_type", columnList = "type"),
        @Index(name = "idx_credit_adjustments_reference", columnList = "reference_from, reference_id"),
        @Index(name = "idx_credit_adjustments_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditAdjustment {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Wallet whose credit limit was adjusted */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, comment = "Foreign key to wallets.id - wallet whose credit limit was adjusted")
    private Wallet wallet;

    /** Credit limit before this adjustment */
    @Column(name = "credit_limit_before", nullable = false, precision = 15, scale = 2, comment = "Credit limit before this adjustment")
    private BigDecimal creditLimitBefore = BigDecimal.ZERO;

    /** Credit limit after this adjustment */
    @Column(name = "credit_limit_after", nullable = false, precision = 15, scale = 2, comment = "Credit limit after this adjustment")
    private BigDecimal creditLimitAfter = BigDecimal.ZERO;

    /** Amount of adjustment (positive for increase, negative for decrease) */
    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2, comment = "Amount of adjustment (positive for increase, negative for decrease)")
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    /** Adjustment type: INCREASE, DECREASE, SET */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10, comment = "Adjustment type: INCREASE (add amount), DECREASE (subtract amount), SET (replace value)")
    private CreditAdjustmentType type;

    /** Reason for the adjustment */
    @Column(name = "reason", nullable = false, length = 255, comment = "Reason for the adjustment")
    private String reason;

    /** Source system (e.g., ADMIN, PRICING_PLAN) */
    @Column(name = "reference_from", nullable = false, length = 100, comment = "Source system (e.g., ADMIN, PRICING_PLAN)")
    private String referenceFrom;

    /** Reference ID from the source system */
    @Column(name = "reference_id", nullable = false, length = 100, comment = "Reference ID from the source system (e.g., wallet_plan.id)")
    private String referenceId;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** User or system that performed the adjustment */
    @Column(name = "created_by", nullable = false, length = 100, comment = "User or system that performed the adjustment")
    private String createdBy;
}
