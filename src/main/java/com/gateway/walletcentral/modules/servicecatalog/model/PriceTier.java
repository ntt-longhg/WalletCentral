package com.gateway.walletcentral.modules.servicecatalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * PriceTier entity - defines tiered pricing within a ServicePrice.
 * Each tier has a basic fee and extended pricing for additional units.
 */
@Entity
@Table(name = "price_tiers", comment = "Defines tiered pricing within a service price. Each tier has a basic fee and extended pricing for additional units.", uniqueConstraints = {
        @UniqueConstraint(name = "uq_price_tiers_service_price_id_tier", columnNames = { "service_price_id", "tier" })
}, indexes = {
        @Index(name = "idx_price_tiers_service_price_id", columnList = "service_price_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceTier {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Associated service price */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_price_id", nullable = false, comment = "Foreign key to service_prices.id - price this tier belongs to")
    private ServicePrice servicePrice;

    /** Tier name/label (e.g., "TIER_1", "TIER_2") */
    @Column(name = "tier", nullable = false, length = 100, comment = "Tier name/label (e.g., TIER_1, TIER_2)")
    private String tier;

    /** Base fee for this tier */
    @Column(name = "basic_fee", nullable = false, precision = 15, scale = 2, comment = "Base fee for this tier")
    private BigDecimal basicFee = BigDecimal.ZERO;

    /** Number of additional units in extended pricing */
    @Column(name = "extended_size", nullable = false, comment = "Number of additional units in extended pricing")
    private Integer extendedSize;

    /** Fee for each extended unit */
    @Column(name = "extended_fee", nullable = false, precision = 15, scale = 2, comment = "Fee for each extended unit beyond the basic tier")
    private BigDecimal extendedFee = BigDecimal.ZERO;
}
