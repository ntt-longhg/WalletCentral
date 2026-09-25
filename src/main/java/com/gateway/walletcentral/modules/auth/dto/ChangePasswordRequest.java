package com.gateway.walletcentral.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authenticated admin password change request")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password must not be blank")
    @Schema(description = "Current password")
    private String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    @Schema(description = "New password (min 8 characters)")
    private String newPassword;
}
