package com.gateway.walletcentral.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public admin login configuration (which login methods are available)")
public class LoginConfigResponse {

    @Schema(description = "Login mode from system config: otp | password | auto", example = "auto")
    private String loginMode;

    @Schema(description = "Whether password login is currently enabled", example = "true")
    private boolean passwordLoginEnabled;

    @Schema(description = "Whether OTP login is currently enabled", example = "true")
    private boolean otpLoginEnabled;

    @Schema(description = "Whether first-time password creation is open", example = "true")
    private boolean passwordSetupOpen;

    @Schema(description = "OTP code length", example = "6")
    private int otpLength;

    @Schema(description = "OTP expiry time in minutes", example = "5")
    private int otpExpiryMinutes;

    @Schema(description = "Minimum seconds between OTP requests", example = "30")
    private long resendCooldownSeconds;

    @Schema(description = "Minimum password length", example = "8")
    private int passwordMinLength;

    @Schema(description = "Comma-separated allowed email domains", example = "dntg.com.vn")
    private String allowedDomains;
}
