package com.gateway.walletcentral.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin password login request")
public class PasswordLoginRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Schema(description = "Admin email address", example = "admin@dntg.com.vn")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Admin password")
    private String password;
}
