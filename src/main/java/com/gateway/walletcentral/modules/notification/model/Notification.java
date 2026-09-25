package com.gateway.walletcentral.modules.notification.model;

import com.gateway.walletcentral.modules.tenant.model.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
    name = "notifications",
    comment = "Real-time notifications with persistence - pushed via WebSocket and stored for history",
    indexes = {
        @Index(name = "idx_notifications_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_notifications_is_read", columnList = "is_read"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at"),
        @Index(name = "idx_notifications_type", columnList = "type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, comment = "Foreign key to tenants.id - target tenant")
    private Tenant tenant;

    @Column(name = "type", nullable = false, length = 50, comment = "Notification type: TRANSACTION, BILLING, INVOICE, WALLET_PLAN, SYSTEM")
    private String type;

    @Column(name = "title", nullable = false, length = 255, comment = "Notification title")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT", comment = "Notification message body")
    private String message;

    @Column(name = "reference_type", length = 50, comment = "Related entity type: TRANSACTION, INVOICE, WALLET_PLAN")
    private String referenceType;

    @Column(name = "reference_id", length = 36, comment = "Related entity ID")
    private String referenceId;

    @Column(name = "is_read", nullable = false, comment = "Whether notification has been read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at", comment = "Timestamp when notification was read")
    private LocalDateTime readAt;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
