package com.gateway.walletcentral.config.encryption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-decrypts all property values wrapped in ENC(...) at application startup.
 *
 * Usage in application.yml:
 *   spring.datasource.password: ENC(encrypted_value_here)
 *
 * Master password is read from:
 *   1. JVM arg:    -Dencryption.master-password=xxx
 *   2. Env var:    ENCRYPTION_MASTER_PASSWORD=xxx
 *   3. Property:   encryption.master-password=xxx (in yml)
 */
public class EncryptedPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final String MASTER_PASSWORD_KEY = "encryption.master-password";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String masterPassword = resolveMasterPassword(environment);
        if (masterPassword == null || masterPassword.isBlank()) {
            return;
        }

        Map<String, Object> decryptedProps = new HashMap<>();

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof MapPropertySource mapSource) {
                for (String name : mapSource.getPropertyNames()) {
                    Object value = mapSource.getProperty(name);
                    if (value instanceof String strValue && isEncrypted(strValue)) {
                        try {
                            String encryptedPayload = extractPayload(strValue);
                            String decrypted = PropertyEncryptor.decrypt(encryptedPayload, masterPassword);
                            decryptedProps.put(name, decrypted);
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    "Failed to decrypt property: " + name + ". " +
                                    "Check your encryption.master-password.", e
                            );
                        }
                    }
                }
            }
        }

        if (!decryptedProps.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("decryptedProperties", decryptedProps));
        }
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    private String extractPayload(String value) {
        return value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length());
    }

    private String resolveMasterPassword(ConfigurableEnvironment environment) {
        // 1. From environment (JVM args -D, env vars, other property sources)
        String password = environment.getProperty(MASTER_PASSWORD_KEY);
        if (password != null && !password.isBlank()) {
            return password;
        }

        // 2. From system property directly (fallback)
        return System.getProperty("encryption.master-password");
    }
}
