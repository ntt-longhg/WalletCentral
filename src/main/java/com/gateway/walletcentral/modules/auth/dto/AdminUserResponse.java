package com.gateway.walletcentral.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin user response with role and permissions")
public class AdminUserResponse {

    @Schema(description = "User ID")
    private String id;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Active status")
    private Boolean isActive;

    @Schema(description = "Assigned role name")
    private String roleName;

    @Schema(description = "Assigned role ID")
    private UUID roleId;

    @Schema(description = "Effective permissions")
    private Set<String> permissions;

    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
}
