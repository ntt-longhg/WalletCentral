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
@Schema(description = "Role response")
public class RoleResponse {

    @Schema(description = "Role ID")
    private UUID id;

    @Schema(description = "Role name", example = "ADMIN")
    private String name;

    @Schema(description = "Role description")
    private String description;

    @Schema(description = "Whether this is a system role (cannot be modified)")
    private Boolean isSystem;

    @Schema(description = "Permission IDs assigned to this role")
    private Set<UUID> permissionIds;

    @Schema(description = "Permission codes assigned to this role")
    private Set<String> permissionCodes;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
