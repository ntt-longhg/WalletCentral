package com.gateway.walletcentral.modules.walletplan.model;

import com.gateway.walletcentral.modules.pricingplan.model.PricingPlan;
import com.gateway.walletcentral.modules.tenant.model.Tenant;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * WalletPlan entity - records a tenant's plan purchase request and approval.
 * Tracks the full lifecycle: PENDING -> APPROVED/REJECTED.
 * Snapshots balance and credit_limit at time of purchase for audit.
 */
@Entity
@Table(name = "wallet_plans", comment = "Records tenant plan purchase requests and approval status. Tracks balance/credit snapshots for audit trail.", indexes = {
        @Index(name = "idx_wallet_plans_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_wallet_plans_pricing_plan_id", columnList = "pricing_plan_id"),
        @Index(name = "idx_wallet_plans_status", columnList = "status"),
        @Index(name = "idx_wallet_plans_created_at", columnList = "created_at"),
        @Index(name = "idx_wallet_plans_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPlan {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Purchased pricing plan */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_plan_id", nullable = false, comment = "Foreign key to pricing_plans.id - plan being purchased")
    private PricingPlan pricingPlan;

    /** Tenant who purchased the plan */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, comment = "Foreign key to tenants.id - tenant who purchased the plan")
    private Tenant tenant;

    /** Price paid for this plan */
    @Column(name = "price", nullable = false, precision = 15, scale = 2, comment = "Price paid for this plan")
    private BigDecimal price = BigDecimal.ZERO;

    /** Bonus amount calculated from plan's bonus_type and bonus_value */
    @Column(name = "bonus_amount", nullable = false, precision = 15, scale = 2, comment = "Bonus amount calculated from plan's bonus_type and bonus_value")
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    /** Total amount credited to wallet (price + bonus) */
    @Column(name = "credited_amount", nullable = false, precision = 15, scale = 2, comment = "Total amount credited to wallet (price + bonus)")
    private BigDecimal creditedAmount = BigDecimal.ZERO;

    /** Plan purchase status: PENDING, APPROVED, REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, comment = "Plan purchase status: PENDING (awaiting approval), APPROVED (completed), REJECTED (denied)")
    private WalletPlanStatus status = WalletPlanStatus.PENDING;

    /** Timestamp when the plan was approved/rejected */
    @Column(name = "approved_at", nullable = true, comment = "Timestamp when the plan was approved/rejected - NULL if pending")
    private LocalDateTime approvedAt = null;

    /** Admin user who approved/rejected the plan */
    @Column(name = "approved_by", nullable = true, length = 100, comment = "Admin user who approved/rejected the plan - NULL if pending")
    private String approvedBy = null;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** User who created this record */
    @Column(name = "created_by", nullable = false, comment = "User who created this record")
    private String createdBy;

    /** Record last update timestamp */
    @Column(name = "updated_at", comment = "Record last update timestamp")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** User who last updated this record */
    @Column(name = "updated_by", nullable = true, comment = "User who last updated this record - NULL if never updated")
    private String updatedBy;

    /** Soft delete timestamp - NULL if not deleted */
    @Column(name = "deleted_at", nullable = true, comment = "Soft delete timestamp - NULL if record is active")
    private LocalDateTime deletedAt = null;
}
