package com.gateway.walletcentral.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
    name = "admin_users",
    comment = "Persistent admin user accounts - RBAC data attached here",
    indexes = {
        @Index(name = "uk_admin_users_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255, comment = "Admin email address")
    private String email;

    @Column(name = "display_name", length = 255, comment = "Display name")
    private String displayName;

    @Column(name = "is_active", nullable = false, comment = "Active status")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "password_hash", length = 255, comment = "BCrypt password hash for password login (NULL = not set)")
    private String passwordHash;

    @Column(name = "last_login_at", comment = "Last login timestamp")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", comment = "Record last update timestamp")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
