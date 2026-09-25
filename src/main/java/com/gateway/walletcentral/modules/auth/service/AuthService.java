package com.gateway.walletcentral.modules.auth.service;

import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.modules.auth.dto.AuthResponse;
import com.gateway.walletcentral.modules.auth.dto.UserRoleAssignRequest;
import com.gateway.walletcentral.modules.auth.model.AdminOtp;
import com.gateway.walletcentral.modules.auth.model.AdminToken;
import com.gateway.walletcentral.modules.auth.model.AdminUser;
import com.gateway.walletcentral.modules.auth.repository.AdminOtpRepository;
import com.gateway.walletcentral.modules.auth.repository.AdminTokenRepository;
import com.gateway.walletcentral.modules.auth.repository.AdminUserRepository;
import com.gateway.walletcentral.modules.auth.repository.RoleRepository;
import com.gateway.walletcentral.modules.systemconfig.model.SystemConfig;
import com.gateway.walletcentral.modules.systemconfig.repository.SystemConfigRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String SUPER_ADMIN_EMAIL = "long.hg@dntg.com.vn";

    private final AdminOtpRepository otpRepository;
    private final AdminTokenRepository tokenRepository;
    private final AdminUserRepository adminUserRepository;
    private final SystemConfigRepository configRepository;
    private final RbacService rbacService;
    private final RoleRepository roleRepository;

    private volatile JavaMailSender cachedMailSender;
    private volatile String cachedMailConfigKey;
    private final ConcurrentHashMap<String, Boolean> otpCooldown = new ConcurrentHashMap<>();

    @Value("${app.mail.host:smtp.gmail.com}")
    private String defaultMailHost;

    @Value("${app.mail.port:587}")
    private int defaultMailPort;

    public AuthService(AdminOtpRepository otpRepository,
            AdminTokenRepository tokenRepository,
            AdminUserRepository adminUserRepository,
            SystemConfigRepository configRepository,
            RbacService rbacService,
            RoleRepository roleRepository) {
        this.otpRepository = otpRepository;
        this.tokenRepository = tokenRepository;
        this.adminUserRepository = adminUserRepository;
        this.configRepository = configRepository;
        this.rbacService = rbacService;
        this.roleRepository = roleRepository;
    }

    public void sendOtp(String email) {
        String allowedDomains = getConfigValue("auth.allowed_domains", "dntg.com.vn");
        validateEmailDomain(email, allowedDomains);

        // Rate limit: 1 OTP per 30 seconds per email
        String cooldownKey = "otp:" + email;
        if (otpCooldown.containsKey(cooldownKey)) {
            throw new BusinessException("OTP_RATE_LIMITED", "Please wait before requesting a new code.");
        }
        otpCooldown.put(cooldownKey, Boolean.TRUE);
        otpCooldown.remove(cooldownKey, Boolean.TRUE);

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

        sendEmailAsync(email, otpCode, expiryMinutes);
    }

    public AuthResponse verifyOtp(String email, String otp) {
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

        String roleName;
        if (SUPER_ADMIN_EMAIL.equalsIgnoreCase(email)) {
            var superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "SUPER_ADMIN role not found"));
            rbacService.assignRoleToUser(email,
                    UserRoleAssignRequest.builder().roleId(superAdminRole.getId()).build());
            roleName = "SUPER_ADMIN";
            if (isFirstLogin) {
                log.info("SUPER_ADMIN role assigned to email: {} (first login)", email);
            }
        } else if (isFirstLogin) {
            var viewerRole = roleRepository.findByName("VIEWER")
                    .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "VIEWER role not found"));
            rbacService.assignRoleToUser(email,
                    UserRoleAssignRequest.builder().roleId(viewerRole.getId()).build());
            roleName = "VIEWER";
            log.info("Default VIEWER role assigned to first-time login: {}", email);
        } else {
            var userInfo = rbacService.getUserInfoByEmail(email);
            roleName = userInfo.getRoleName();
        }

        int tokenExpiryHours = Integer.parseInt(getConfigValue("auth.token_expiry_hours", "24"));
        AdminToken adminToken = AdminToken.builder()
                .token(UUID.randomUUID().toString())
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpiryHours))
                .build();
        tokenRepository.save(adminToken);

        log.info("Admin login successful for email: {} (role={}, firstLogin={})", email, roleName, isFirstLogin);

        Set<String> permissions = rbacService.getPermissionsByEmail(email);

        return AuthResponse.builder()
                .token(adminToken.getToken())
                .email(email)
                .expiresInHours(tokenExpiryHours)
                .roleName(roleName)
                .permissions(permissions)
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
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Async("virtualThreadExecutor")
    public void sendEmailAsync(String toEmail, String otpCode, int expiryMinutes) {
        try {
            JavaMailSender mailSender = getOrCreateMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromEmail = getConfigValue("smtp.from_email", getConfigValue("smtp.username", ""));
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[WalletCenTral] Admin Login OTP Code");
            helper.setText(buildOtpEmailBody(otpCode, expiryMinutes), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
        }
    }

    private synchronized JavaMailSender getOrCreateMailSender() {
        String configKey = getConfigValue("smtp.host", defaultMailHost) + ":"
                + getConfigValue("smtp.port", String.valueOf(defaultMailPort));
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

        org.springframework.mail.javamail.JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol("smtp");

        java.util.Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

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

    private String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .filter(v -> !v.isBlank())
                .orElse(defaultValue);
    }
}
