package com.gateway.walletcentral.modules.servicecatalog.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.ColumnDefault;
import java.util.UUID;

/**
 * ServicePrice entity - defines pricing tiers for a service.
 * Supports initial fee (first N units) and subsequent fee (remaining units).
 * Only one active price per service at a given time via effective_date.
 */
@Entity
@Table(
    name = "service_prices",
    comment = "Defines pricing structure for a service. Supports initial fee (first N units) and subsequent tiered pricing.",
    indexes = {
        @Index(name = "idx_service_prices_service_id", columnList = "service_id"),
        @Index(name = "idx_service_prices_active", columnList = "is_active"),
        @Index(name = "idx_service_prices_effective_date", columnList = "effective_date"),
        @Index(name = "idx_service_prices_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePrice {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Associated service */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false, comment = "Foreign key to services.id - service this price belongs to")
    private Service service;

    /** Number of units included in the initial fee */
    @Column(name = "initial_size", nullable = false, comment = "Number of units included in the initial fee")
    private Integer initialSize;

    /** Fee charged for the initial units */
    @Column(name = "initial_fee", nullable = false, precision = 15, scale = 2, comment = "Fee charged for the initial units")
    private BigDecimal initialFee = BigDecimal.ZERO;

    /** Number of units in each subsequent tier */
    @Column(name = "subsequent_size", nullable = false, comment = "Number of units in each subsequent tier")
    private Integer subsequentSize;

    /** Fee charged for each subsequent tier */
    @Column(name = "subsequent_fee", nullable = false, precision = 15, scale = 2, comment = "Fee charged for each subsequent tier")
    private BigDecimal subsequentFee = BigDecimal.ZERO;

    /** Whether this price is currently active */
    @Column(name = "is_active", nullable = true, comment = "Whether this price is currently active (1=active, 0=inactive)")
    @ColumnDefault("1")
    private Boolean isActive = true;

    /** Date from which this price becomes effective */
    @Column(name = "effective_date", nullable = false, comment = "Date from which this price becomes effective")
    private LocalDateTime effectiveDate;

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
