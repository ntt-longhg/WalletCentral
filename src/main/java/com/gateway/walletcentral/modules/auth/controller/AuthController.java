package com.gateway.walletcentral.modules.auth.controller;

import com.gateway.walletcentral.config.SecurityConfig;
import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.auth.dto.AuthResponse;
import com.gateway.walletcentral.modules.auth.dto.ChangePasswordRequest;
import com.gateway.walletcentral.modules.auth.dto.LoginConfigResponse;
import com.gateway.walletcentral.modules.auth.dto.PasswordLoginRequest;
import com.gateway.walletcentral.modules.auth.dto.SendOtpRequest;
import com.gateway.walletcentral.modules.auth.dto.SetupPasswordRequest;
import com.gateway.walletcentral.modules.auth.dto.UserInfoResponse;
import com.gateway.walletcentral.modules.auth.dto.VerifyOtpRequest;
import com.gateway.walletcentral.modules.auth.service.AuthService;
import com.gateway.walletcentral.modules.auth.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Admin authentication operations (OTP and password)")
public class AuthController {

    private final AuthService authService;
    private final RbacService rbacService;

    public AuthController(AuthService authService, RbacService rbacService) {
        this.authService = authService;
        this.rbacService = rbacService;
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP code to admin email")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getEmail());
        Map<String, String> data = Map.of(
                "email", request.getEmail(),
                "message", "OTP code has been sent to your email"
        );
        return ResponseEntity.ok(ApiResponse.ok(data, "OTP sent successfully"));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP code and get admin session token")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @GetMapping("/login-config")
    @Operation(summary = "Get public login configuration (available login methods)")
    public ResponseEntity<ApiResponse<LoginConfigResponse>> getLoginConfig() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getLoginConfig()));
    }

    @PostMapping("/password/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest request) {
        AuthResponse response = authService.loginWithPassword(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @PostMapping("/password/setup")
    @Operation(summary = "First-time password creation without login (once per account)")
    public ResponseEntity<ApiResponse<AuthResponse>> setupPassword(
            @Valid @RequestBody SetupPasswordRequest request) {
        AuthResponse response = authService.setupPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(response, "Password created successfully"));
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change password (authenticated, revokes all sessions)")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest changeRequest) {
        String email = (String) request.getAttribute(SecurityConfig.REQUEST_ATTR_EMAIL);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        authService.changePassword(email, changeRequest.getOldPassword(), changeRequest.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("message", "Password changed. Please login again."), "Password changed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout admin - revoke session token")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("X-API-Key") String token) {
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Manual cleanup of expired OTPs and tokens (users are preserved)")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> cleanup() {
        int otps = authService.cleanupExpiredOtps();
        int tokens = authService.cleanupExpiredTokens();
        Map<String, Integer> data = Map.of("deletedOtps", otps, "deletedTokens", tokens);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cleanup completed"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info with role and permissions")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            HttpServletRequest request) {
        String email = (String) request.getAttribute(SecurityConfig.REQUEST_ATTR_EMAIL);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        UserInfoResponse response = rbacService.getUserInfoByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
