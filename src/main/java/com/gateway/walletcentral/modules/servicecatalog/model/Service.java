package com.gateway.walletcentral.modules.servicecatalog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Service entity - represents a billable service offered to tenants.
 * Each service has its own pricing structure via ServicePrice and PriceTier.
 */
@Entity
@Table(
    name = "service_catalogs",
    comment = "Defines billable services offered to tenants. Each service has its own pricing structure via service_prices and price_tiers.",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_catalogs_code", columnNames = "code")
    },
    indexes = {
        @Index(name = "idx_service_catalogs_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Unique service code - used as identifier in API calls */
    @Column(name = "code", nullable = false, unique = true, length = 100, comment = "Unique service code - used as identifier in API calls")
    private String code;

    /** Service display name */
    @Column(name = "name", nullable = false, comment = "Service display name")
    private String name;

    /** Optional description of the service */
    @Column(name = "description", nullable = true, columnDefinition = "TEXT", comment = "Optional description of the service")
    private String description = null;

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
