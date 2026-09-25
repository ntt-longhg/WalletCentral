package com.gateway.walletcentral.modules.tenant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Tenant entity - represents a business unit (customer) in the billing system.
 * Each tenant has unique client credentials for OAuth2 authentication.
 */
@Entity
@Table(
    name = "tenants",
    comment = "Stores tenant (customer) information. Each tenant represents a business unit with unique OAuth2 credentials.",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenants_client_id", columnNames = "client_id")
    },
    indexes = {
        @Index(name = "idx_tenants_status", columnList = "status"),
        @Index(name = "idx_tenants_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    /** Unique identifier (UUID v7) */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    /** Tenant display name */
    @Column(name = "name", nullable = false, length = 100, comment = "Tenant display name")
    private String name;

    /** OAuth2 client ID - unique per tenant */
    @Column(name = "client_id", nullable = false, unique = true, length = 100, comment = "OAuth2 client ID - unique identifier for API authentication")
    private String clientId;

    /** OAuth2 client secret - hashed */
    @Column(name = "client_secret", nullable = false, comment = "OAuth2 client secret - hashed value for API authentication")
    private String clientSecret;

    /** Comma-separated list of allowed domains for CORS */
    @Column(name = "allowed_domains", nullable = false, columnDefinition = "TEXT", comment = "Comma-separated list of allowed domains for CORS policy")
    private String allowedDomains;

    /** Tenant status: ACTIVE, INACTIVE, SUSPENDED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, comment = "Tenant status: ACTIVE, INACTIVE, SUSPENDED")
    private TenantStatus status = TenantStatus.ACTIVE;

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
