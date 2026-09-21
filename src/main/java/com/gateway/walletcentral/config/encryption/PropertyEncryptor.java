package com.gateway.walletcentral.config.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 GCM encryption utility for property values.
 * Format: salt:iv:ciphertext (all Base64-encoded)
 */
public final class PropertyEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String KEY_FACTORY = "PBKDF2WithHmacSHA256";
    private static final String SEPARATOR = ":";

    private PropertyEncryptor() {
    }

    /**
     * Derive AES key from master password + salt.
     */
    private static SecretKey deriveKey(String masterPassword, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                masterPassword.toCharArray(),
                salt,
                KEY_ITERATIONS,
                KEY_LENGTH
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypt plaintext using master password.
     * Returns Base64(salt):Base64(iv):Base64(ciphertext)
     */
    public static String encrypt(String plaintext, String masterPassword) throws Exception {
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(salt) + SEPARATOR
                + Base64.getEncoder().encodeToString(iv) + SEPARATOR
                + Base64.getEncoder().encodeToString(ciphertext);
    }

    /**
     * Decrypt ciphertext using master password.
     * Input: Base64(salt):Base64(iv):Base64(ciphertext)
     */
    public static String decrypt(String encrypted, String masterPassword) throws Exception {
        String[] parts = encrypted.split(SEPARATOR);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted format. Expected: salt:iv:ciphertext");
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
