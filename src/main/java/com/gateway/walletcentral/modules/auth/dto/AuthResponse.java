package com.gateway.walletcentral.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Schema(description = "Admin session token", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @Schema(description = "Admin email address", example = "admin@dntg.com.vn")
    private String email;

    @Schema(description = "Token expiry time in hours", example = "24")
    private int expiresInHours;

    @Schema(description = "User role name", example = "SUPER_ADMIN")
    private String roleName;

    @Schema(description = "Effective permissions for this user")
    private Set<String> permissions;

    @Schema(description = "How the user logged in: OTP | PASSWORD", example = "OTP")
    private String loginMethod;
}
