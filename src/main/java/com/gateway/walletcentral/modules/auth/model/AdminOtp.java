package com.gateway.walletcentral.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
    name = "admin_otps",
    comment = "Stores temporary OTP codes for admin email verification",
    indexes = {
        @Index(name = "idx_admin_otps_email", columnList = "email"),
        @Index(name = "idx_admin_otps_expires_at", columnList = "expires_at"),
        @Index(name = "idx_admin_otps_email_used", columnList = "email, used")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOtp {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @Column(name = "email", nullable = false, length = 255, comment = "Email address receiving the OTP")
    private String email;

    @Column(name = "otp_code", nullable = false, length = 10, comment = "OTP verification code")
    private String otpCode;

    @Column(name = "expires_at", nullable = false, comment = "OTP expiration timestamp")
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0", comment = "Whether OTP has been used (0=unused, 1=used)")
    @Builder.Default
    private Boolean used = false;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
