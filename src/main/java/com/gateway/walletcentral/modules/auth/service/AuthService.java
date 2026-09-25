package com.gateway.walletcentral.modules.auth.service;

import com.gateway.walletcentral.core.config.AppConfigCache;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.modules.auth.dto.AuthResponse;
import com.gateway.walletcentral.modules.auth.dto.LoginConfigResponse;
import com.gateway.walletcentral.modules.auth.dto.UserRoleAssignRequest;
import com.gateway.walletcentral.modules.auth.model.AdminOtp;
import com.gateway.walletcentral.modules.auth.model.AdminToken;
import com.gateway.walletcentral.modules.auth.model.AdminUser;
import com.gateway.walletcentral.modules.auth.repository.AdminOtpRepository;
import com.gateway.walletcentral.modules.auth.repository.AdminTokenRepository;
import com.gateway.walletcentral.modules.auth.repository.AdminUserRepository;
import com.gateway.walletcentral.modules.auth.repository.RoleRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_SUPER_ADMIN_EMAIL = "long.hg@dntg.com.vn";

    // Brute-force guard defaults (overridable via system_config, applied immediately)
    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    private static final long DEFAULT_FAIL_WINDOW_MINUTES = 10;
    private static final long DEFAULT_LOCK_MINUTES = 10;

    private final AdminOtpRepository otpRepository;
    private final AdminTokenRepository tokenRepository;
    private final AdminUserRepository adminUserRepository;
    private final AppConfigCache configCache;
    private final RbacService rbacService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private volatile JavaMailSender cachedMailSender;
    private volatile String cachedMailConfigKey;

    // OTP resend cooldown: email -> last sent time (30s)
    private final ConcurrentHashMap<String, LocalDateTime> otpCooldown = new ConcurrentHashMap<>();

    // Password brute-force guard: email -> failure state
    private final ConcurrentHashMap<String, FailedLoginAttempt> failedLogins = new ConcurrentHashMap<>();

    private record FailedLoginAttempt(int count, LocalDateTime windowStart, LocalDateTime lockedUntil) {
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.mail.host:smtp.gmail.com}")
    private String defaultMailHost;

    @Value("${app.mail.port:587}")
    private int defaultMailPort;

    public AuthService(AdminOtpRepository otpRepository,
            AdminTokenRepository tokenRepository,
            AdminUserRepository adminUserRepository,
            AppConfigCache configCache,
            RbacService rbacService,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.tokenRepository = tokenRepository;
        this.adminUserRepository = adminUserRepository;
        this.configCache = configCache;
        this.rbacService = rbacService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== Login mode (OTP / password fallback) ==========

    public LoginConfigResponse getLoginConfig() {
        String mode = getLoginMode();
        boolean passwordEnabled = !"otp".equals(mode);
        boolean otpEnabled = !"password".equals(mode);
        return LoginConfigResponse.builder()
                .loginMode(mode)
                .passwordLoginEnabled(passwordEnabled)
                .otpLoginEnabled(otpEnabled)
                .passwordSetupOpen(passwordEnabled && isPasswordSetupOpen())
                .otpLength(getIntConfig("otp.length", 6))
                .otpExpiryMinutes(getIntConfig("otp.expiry_minutes", 5))
                .resendCooldownSeconds(getLongConfig("otp.resend_cooldown_seconds", 30))
                .passwordMinLength(getIntConfig("auth.password_min_length", 8))
                .allowedDomains(getConfigValue("auth.allowed_domains", "dntg.com.vn"))
                .build();
    }

    private String getLoginMode() {
        String mode = getConfigValue("auth.login_mode", "auto").trim().toLowerCase();
        return switch (mode) {
            case "otp", "password", "auto" -> mode;
            default -> {
                log.warn("Unknown auth.login_mode '{}', falling back to 'auto'", mode);
                yield "auto";
            }
        };
    }

    private boolean isPasswordSetupOpen() {
        return "open".equalsIgnoreCase(getConfigValue("auth.password_setup", "open").trim());
    }

    private void requirePasswordLoginEnabled() {
        if ("otp".equals(getLoginMode())) {
            throw new BusinessException("PASSWORD_LOGIN_DISABLED",
                    "Password login is disabled. Please login with OTP.");
        }
    }

    // ========== Password login ==========

    public AuthResponse loginWithPassword(String email, String rawPassword) {
        requirePasswordLoginEnabled();
        String allowedDomains = getConfigValue("auth.allowed_domains", "dntg.com.vn");
        validateEmailDomain(email, allowedDomains);
        checkLocked(email);

        AdminUser adminUser = adminUserRepository.findByEmail(email).orElse(null);
        boolean ok = adminUser != null
                && Boolean.TRUE.equals(adminUser.getIsActive())
                && adminUser.getPasswordHash() != null
                && passwordEncoder.matches(rawPassword, adminUser.getPasswordHash());
        if (!ok) {
            recordFailedAttempt(email);
            // Generic message: do not reveal which check failed
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password.");
        }
        clearFailedAttempts(email);

        adminUser.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(adminUser);

        String roleName = resolveRoleOnLogin(email, false);
        log.info("Admin password login successful for email: {} (role={})", email, roleName);
        return buildAuthResponse(email, roleName, "PASSWORD");
    }

    /**
     * First-time password creation without prior login. Allowed only when password
     * login is enabled, setup is open, and the account has no password yet.
     * Auto-creates the admin user (same as OTP first login).
     */
    public AuthResponse setupPassword(String email, String rawPassword) {
        requirePasswordLoginEnabled();
        if (!isPasswordSetupOpen()) {
            throw new BusinessException("PASSWORD_SETUP_CLOSED",
                    "First-time password setup is closed. Please contact an administrator.");
        }
        String allowedDomains = getConfigValue("auth.allowed_domains", "dntg.com.vn");
        validateEmailDomain(email, allowedDomains);
        validatePasswordPolicy(rawPassword);

        boolean isFirstLogin = !adminUserRepository.existsByEmail(email);
        AdminUser adminUser;
        if (isFirstLogin) {
            adminUser = adminUserRepository.save(AdminUser.builder()
                    .email(email)
                    .displayName(extractDisplayName(email))
                    .isActive(true)
                    .build());
        } else {
            adminUser = adminUserRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Admin user not found."));
            if (!Boolean.TRUE.equals(adminUser.getIsActive())) {
                throw new BusinessException("USER_DISABLED", "Admin account is disabled.");
            }
            if (adminUser.getPasswordHash() != null) {
                throw new BusinessException("PASSWORD_ALREADY_SET",
                        "Password is already set. Please login or use change password.");
            }
        }

        adminUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        adminUser.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(adminUser);

        String roleName = resolveRoleOnLogin(email, isFirstLogin);
        log.info("Admin password setup successful for email: {} (role={}, firstLogin={})",
                email, roleName, isFirstLogin);
        return buildAuthResponse(email, roleName, "PASSWORD");
    }

    /**
     * Authenticated password change. Revokes all other sessions for security.
     */
    public void changePassword(String email, String oldPassword, String newPassword) {
        requirePasswordLoginEnabled();
        AdminUser adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Admin user not found."));
        if (adminUser.getPasswordHash() == null
                || !passwordEncoder.matches(oldPassword, adminUser.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Current password is incorrect.");
        }
        validatePasswordPolicy(newPassword);
        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(adminUser);
        tokenRepository.deleteByEmail(email);
        log.info("Admin password changed for email: {}, all sessions revoked", email);
    }

    private void validatePasswordPolicy(String rawPassword) {
        int minLength = getIntConfig("auth.password_min_length", 8);
        int maxLength = getIntConfig("auth.password_max_length", 72);
        if (rawPassword == null || rawPassword.length() < minLength || rawPassword.length() > maxLength) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Password must be " + minLength + "-" + maxLength + " characters.");
        }
    }

    private void checkLocked(String email) {
        FailedLoginAttempt attempt = failedLogins.get(email.toLowerCase());
        if (attempt != null && attempt.lockedUntil() != null
                && LocalDateTime.now().isBefore(attempt.lockedUntil())) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Too many failed attempts. Please try again after " + attempt.lockedUntil() + ".");
        }
    }

    private void recordFailedAttempt(String email) {
        String key = email.toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        int maxAttempts = getIntConfig("auth.max_failed_attempts", DEFAULT_MAX_FAILED_ATTEMPTS);
        long windowMinutes = getLongConfig("auth.fail_window_minutes", DEFAULT_FAIL_WINDOW_MINUTES);
        long lockMinutes = getLongConfig("auth.lock_minutes", DEFAULT_LOCK_MINUTES);
        failedLogins.compute(key, (k, prev) -> {
            if (prev == null || prev.windowStart().plusMinutes(windowMinutes).isBefore(now)) {
                return new FailedLoginAttempt(1, now, null);
            }
            int count = prev.count() + 1;
            LocalDateTime lockedUntil = count >= maxAttempts ? now.plusMinutes(lockMinutes) : null;
            if (lockedUntil != null) {
                log.warn("Admin account locked due to brute-force: email={}", email);
            }
            return new FailedLoginAttempt(count, prev.windowStart(), lockedUntil);
        });
    }

    private void clearFailedAttempts(String email) {
        failedLogins.remove(email.toLowerCase());
    }

    public void sendOtp(String email) {
        if ("password".equals(getLoginMode())) {
            throw new BusinessException("OTP_LOGIN_DISABLED",
                    "OTP login is disabled. Please login with password.");
        }
        String allowedDomains = getConfigValue("auth.allowed_domains", "dntg.com.vn");
        validateEmailDomain(email, allowedDomains);

        // Rate limit: configurable cooldown between OTP requests per email
        long cooldownSeconds = getLongConfig("otp.resend_cooldown_seconds", 30);
        LocalDateTime lastSent = otpCooldown.get(email.toLowerCase());
        if (lastSent != null && lastSent.plusSeconds(cooldownSeconds).isAfter(LocalDateTime.now())) {
            throw new BusinessException("OTP_RATE_LIMITED", "Please wait before requesting a new code.");
        }

        int otpLength = Integer.parseInt(getConfigValue("otp.length", "6"));
        String otpCode = generateOtp(otpLength);
        int expiryMinutes = Integer.parseInt(getConfigValue("otp.expiry_minutes", "5"));

        otpRepository.markAllUsedByEmail(email);

        AdminOtp adminOtp = AdminOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();
        otpRepository.save(adminOtp);
        otpCooldown.put(email.toLowerCase(), LocalDateTime.now());

        // Synchronous send: SMTP failure must surface so callers (auto mode)
        // can fall back to password login instead of reporting false success.
        sendOtpEmail(email, otpCode, expiryMinutes);
    }

    public AuthResponse verifyOtp(String email, String otp) {
        if ("password".equals(getLoginMode())) {
            throw new BusinessException("OTP_LOGIN_DISABLED",
                    "OTP login is disabled. Please login with password.");
        }
        AdminOtp adminOtp = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException("OTP_NOT_FOUND", "No OTP found. Please request a new code."));

        if (adminOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP_EXPIRED", "OTP has expired. Please request a new code.");
        }

        if (!adminOtp.getOtpCode().equals(otp)) {
            throw new BusinessException("OTP_INVALID", "Invalid OTP code. Please try again.");
        }

        adminOtp.setUsed(true);
        otpRepository.save(adminOtp);

        // Check if first login and find/create user in parallel
        boolean isFirstLogin = !adminUserRepository.existsByEmail(email);
        AdminUser adminUser = adminUserRepository.findByEmail(email).orElseGet(() -> {
            AdminUser newUser = AdminUser.builder()
                    .email(email)
                    .displayName(extractDisplayName(email))
                    .isActive(true)
                    .build();
            return adminUserRepository.save(newUser);
        });

        adminUser.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(adminUser);

        String roleName = resolveRoleOnLogin(email, isFirstLogin);
        log.info("Admin login successful for email: {} (role={}, firstLogin={})", email, roleName, isFirstLogin);

        return buildAuthResponse(email, roleName, "OTP");
    }

    private String resolveRoleOnLogin(String email, boolean isFirstLogin) {
        String superAdminEmail = getConfigValue("auth.super_admin_email", DEFAULT_SUPER_ADMIN_EMAIL);
        String superAdminRole = getConfigValue("auth.super_admin_role", "SUPER_ADMIN");
        String defaultRole = getConfigValue("auth.default_role", "VIEWER");
        String roleName;
        if (superAdminEmail.equalsIgnoreCase(email)) {
            var role = roleRepository.findByName(superAdminRole)
                    .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", superAdminRole + " role not found"));
            rbacService.assignRoleToUser(email,
                    UserRoleAssignRequest.builder().roleId(role.getId()).build());
            roleName = superAdminRole;
            if (isFirstLogin) {
                log.info("SUPER_ADMIN role assigned to email: {} (first login)", email);
            }
        } else if (isFirstLogin) {
            var role = roleRepository.findByName(defaultRole)
                    .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", defaultRole + " role not found"));
            rbacService.assignRoleToUser(email,
                    UserRoleAssignRequest.builder().roleId(role.getId()).build());
            roleName = defaultRole;
            log.info("Default {} role assigned to first-time login: {}", defaultRole, email);
        } else {
            var userInfo = rbacService.getUserInfoByEmail(email);
            roleName = userInfo.getRoleName();
        }
        return roleName;
    }

    private AuthResponse buildAuthResponse(String email, String roleName, String loginMethod) {
        int tokenExpiryHours = Integer.parseInt(getConfigValue("auth.token_expiry_hours", "24"));
        AdminToken adminToken = AdminToken.builder()
                .token(UUID.randomUUID().toString())
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpiryHours))
                .build();
        tokenRepository.save(adminToken);

        Set<String> permissions = rbacService.getPermissionsByEmail(email);

        return AuthResponse.builder()
                .token(adminToken.getToken())
                .email(email)
                .expiresInHours(tokenExpiryHours)
                .roleName(roleName)
                .permissions(permissions)
                .loginMethod(loginMethod)
                .build();
    }

    public void logout(String token) {
        tokenRepository.deleteByToken(token);
        log.info("Admin token revoked: {}", token);
    }

    @Transactional(readOnly = true)
    public boolean isValidAdminToken(String token) {
        return tokenRepository.existsByTokenAndExpiresAtAfter(token, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public String getEmailFromToken(String token) {
        return tokenRepository.findByToken(token)
                .map(AdminToken::getEmail)
                .orElse(null);
    }

    public int cleanupExpiredOtps() {
        return otpRepository.deleteExpired(LocalDateTime.now());
    }

    public int cleanupExpiredTokens() {
        return tokenRepository.deleteExpired(LocalDateTime.now());
    }

    private String extractDisplayName(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        return localPart.replace('.', ' ').replace('_', ' ');
    }

    private void validateEmailDomain(String email, String allowedDomains) {
        String[] domains = allowedDomains.split(",");
        boolean allowed = false;
        for (String domain : domains) {
            String trimmed = domain.trim().toLowerCase();
            if (email.toLowerCase().endsWith("@" + trimmed)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BusinessException("EMAIL_NOT_ALLOWED",
                    "Email domain not allowed. Allowed domains: " + allowedDomains);
        }
    }

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public void sendOtpEmail(String toEmail, String otpCode, int expiryMinutes) {
        try {
            JavaMailSender mailSender = getOrCreateMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromEmail = getConfigValue("smtp.from-email", getConfigValue("smtp.username", ""));
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(getConfigValue("smtp.otp_subject", "[WalletCenTral] Admin Login OTP Code"));
            helper.setText(buildOtpEmailBody(otpCode, expiryMinutes), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new BusinessException("SMTP_SEND_FAILED",
                    "Could not send OTP email. Mail server may be unreachable; try password login instead.");
        }
    }

    private synchronized JavaMailSender getOrCreateMailSender() {
        String configKey = getConfigValue("smtp.host", defaultMailHost) + ":"
                + getConfigValue("smtp.port", String.valueOf(defaultMailPort)) + ":"
                + getConfigValue("smtp.username", "") + ":"
                + getConfigValue("smtp.password", "").hashCode() + ":"
                + getConfigValue("smtp.conn_timeout_ms", "5000") + ":"
                + getConfigValue("smtp.read_timeout_ms", "5000") + ":"
                + getConfigValue("smtp.starttls_enable", "true");
        if (cachedMailSender == null || !configKey.equals(cachedMailConfigKey)) {
            cachedMailSender = buildMailSender();
            cachedMailConfigKey = configKey;
        }
        return cachedMailSender;
    }

    private JavaMailSender buildMailSender() {
        String host = getConfigValue("smtp.host", defaultMailHost);
        int port = Integer.parseInt(getConfigValue("smtp.port", String.valueOf(defaultMailPort)));
        String username = getConfigValue("smtp.username", "");
        String password = getConfigValue("smtp.password", "");
        int connTimeout = getIntConfig("smtp.conn_timeout_ms", 5000);
        int readTimeout = getIntConfig("smtp.read_timeout_ms", 5000);
        boolean starttls = getBooleanConfig("smtp.starttls_enable", true);

        org.springframework.mail.javamail.JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol("smtp");

        java.util.Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.starttls.required", String.valueOf(starttls));
        props.put("mail.smtp.connectiontimeout", String.valueOf(connTimeout));
        props.put("mail.smtp.timeout", String.valueOf(readTimeout));

        return mailSender;
    }

    private String buildOtpEmailBody(String otpCode, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 500px; margin: auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="background: #2563eb; padding: 24px; text-align: center;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 20px;">WalletCentral Platform</h1>
                    </div>
                    <div style="padding: 32px 24px; text-align: center;">
                      <p style="color: #475569; font-size: 14px; margin-bottom: 24px;">Your one-time verification code is:</p>
                      <div style="background: #f1f5f9; border-radius: 8px; padding: 16px 24px; margin-bottom: 24px;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1e293b;">%s</span>
                      </div>
                      <p style="color: #94a3b8; font-size: 12px; margin-bottom: 0;">This code expires in %d minutes.</p>
                      <p style="color: #94a3b8; font-size: 12px; margin-top: 8px;">If you did not request this code, please ignore this email.</p>
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(otpCode, expiryMinutes);
    }

    // All runtime reads are served from the local AppConfigCache (no DB pressure).
    // UI edits take effect after POST /api/v1/system-configs/reload.
    private String getConfigValue(String key, String defaultValue) {
        return configCache.getValue(key, defaultValue);
    }

    private int getIntConfig(String key, int defaultValue) {
        return configCache.getInt(key, defaultValue);
    }

    private long getLongConfig(String key, long defaultValue) {
        return configCache.getLong(key, defaultValue);
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        return configCache.getBoolean(key, defaultValue);
    }
}
