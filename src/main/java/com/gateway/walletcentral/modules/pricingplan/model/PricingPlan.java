package com.gateway.walletcentral.modules.pricingplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * PricingPlan entity - defines a purchasable plan for tenants.
 * Two types: BALANCE_TOPUP (adds to balance) and CREDIT_INCREASE (adds to credit limit).
 * Plans go through approval workflow via WalletPlan.
 */
@Entity
@Table(
    name = "pricing_plans",
    comment = "Defines purchasable plans for tenants. Two types: BALANCE_TOPUP (add to balance) and CREDIT_INCREASE (add to credit limit).",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pricing_plans_code", columnNames = "code")
    },
    indexes = {
        @Index(name = "idx_pricing_plans_type", columnList = "type"),
        @Index(name = "idx_pricing_plans_status", columnList = "status"),
        @Index(name = "idx_pricing_plans_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPlan {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Unique plan code - used as identifier in API calls */
    @Column(name = "code", nullable = false, unique = true, length = 100, comment = "Unique plan code - used as identifier in API calls")
    private String code;

    /** Plan display name */
    @Column(name = "name", nullable = false, comment = "Plan display name")
    private String name;

    /** Optional plan description */
    @Column(name = "description", nullable = true, columnDefinition = "TEXT", comment = "Optional plan description")
    private String description = null;

    /** Price to purchase this plan */
    @Column(name = "price", nullable = false, precision = 15, scale = 2, comment = "Price to purchase this plan")
    private BigDecimal price = BigDecimal.ZERO;

    /** Plan type: BALANCE_TOPUP (add to balance) or CREDIT_INCREASE (add to credit limit) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, comment = "Plan type: BALANCE_TOPUP (add to balance), CREDIT_INCREASE (add to credit limit)")
    private PricingPlanType type = PricingPlanType.BALANCE_TOPUP;

    /** Bonus calculation type: PERCENTAGE or FIXED_AMOUNT */
    @Enumerated(EnumType.STRING)
    @Column(name = "bonus_type", nullable = false, length = 20, comment = "Bonus calculation type: PERCENTAGE (e.g., 10%), FIXED_AMOUNT (e.g., $5)")
    private BonusType bonusType = BonusType.PERCENTAGE;

    /** Bonus value - percentage (e.g., 10 = 10%) or fixed amount */
    @Column(name = "bonus_value", nullable = true, precision = 15, scale = 2, comment = "Bonus value - percentage (e.g., 10 = 10%) or fixed amount")
    private BigDecimal bonusValue = BigDecimal.ZERO;

    /** Credit limit action when plan is approved: INCREASE, SET, or NONE */
    @Enumerated(EnumType.STRING)
    @Column(name = "credit_limit_action", nullable = false, length = 10, comment = "Credit limit action when plan is approved: INCREASE (add), SET (replace), NONE (no change)")
    private CreditLimitAction creditLimitAction = CreditLimitAction.NONE;

    /** Credit limit value to add/set when plan is approved (used with CREDIT_INCREASE type) */
    @Column(name = "credit_limit_value", nullable = false, precision = 15, scale = 2, comment = "Credit limit value to add/set when plan is approved (used with CREDIT_INCREASE type)")
    private BigDecimal creditLimitValue = BigDecimal.ZERO;

    /** Plan status: ACTIVE, INACTIVE */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, comment = "Plan status: ACTIVE (available for purchase), INACTIVE (hidden)")
    private PricingPlanStatus status = PricingPlanStatus.ACTIVE;

    /** Record creation timestamp */
    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Record last update timestamp */
    @Column(name = "updated_at", comment = "Record last update timestamp")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Soft delete timestamp - NULL if not deleted */
    @Column(name = "deleted_at", nullable = true, comment = "Soft delete timestamp - NULL if record is active")
    private LocalDateTime deletedAt = null;
}
