package com.gateway.walletcentral.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
    name = "admin_tokens",
    comment = "Stores active admin session tokens for API authentication",
    indexes = {
        @Index(name = "idx_admin_tokens_email", columnList = "email"),
        @Index(name = "idx_admin_tokens_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminToken {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 36, comment = "Session token UUID string")
    private String token;

    @Column(name = "email", nullable = false, length = 255, comment = "Admin email address")
    private String email;

    @Column(name = "expires_at", nullable = false, comment = "Token expiration timestamp")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
