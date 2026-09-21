package com.gateway.walletcentral.config.encryption;

import java.io.Console;
import java.util.Scanner;

/**
 * CLI tool to encrypt/decrypt property values.
 *
 * Usage:
 *   java -cp walletcentral.jar com.gateway.walletcentral.config.encryption.EncryptCLI encrypt
 *   java -cp walletcentral.jar com.gateway.walletcentral.config.encryption.EncryptCLI decrypt <encrypted_value>
 */
public class EncryptCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        Scanner scanner = new Scanner(System.in);

        switch (command) {
            case "encrypt" -> {
                System.out.print("Enter master password: ");
                String masterPassword = readPassword(scanner);
                System.out.print("Enter value to encrypt: ");
                String plaintext = scanner.nextLine().trim();

                try {
                    String encrypted = PropertyEncryptor.encrypt(plaintext, masterPassword);
                    System.out.println();
                    System.out.println("=== Encrypted value (copy this into ENC()): ===");
                    System.out.println("ENC(" + encrypted + ")");
                    System.out.println();
                } catch (Exception e) {
                    System.err.println("Encryption failed: " + e.getMessage());
                }
            }

            case "decrypt" -> {
                if (args.length < 2) {
                    System.err.println("Usage: decrypt <encrypted_value>");
                    return;
                }
                System.out.print("Enter master password: ");
                String masterPassword = readPassword(scanner);

                String encrypted = args[1];
                // Strip ENC() wrapper if provided
                if (encrypted.startsWith("ENC(") && encrypted.endsWith(")")) {
                    encrypted = encrypted.substring(4, encrypted.length() - 1);
                }

                try {
                    String decrypted = PropertyEncryptor.decrypt(encrypted, masterPassword);
                    System.out.println("Decrypted value: " + decrypted);
                } catch (Exception e) {
                    System.err.println("Decryption failed: " + e.getMessage());
                }
            }

            default -> printUsage();
        }
    }

    private static String readPassword(Scanner scanner) {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword();
            return new String(password);
        }
        return scanner.nextLine().trim();
    }

    private static void printUsage() {
        System.out.println("=== WalletCentral Property Encryptor ===");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  encrypt          Encrypt a plaintext value");
        System.out.println("  decrypt <value>  Decrypt an encrypted value");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -cp walletcentral.jar \\");
        System.out.println("    com.gateway.walletcentral.config.encryption.EncryptCLI encrypt");
        System.out.println();
        System.out.println("  java -cp walletcentral.jar \\");
        System.out.println("    com.gateway.walletcentral.config.encryption.EncryptCLI decrypt 'salt:iv:cipher'");
    }
}
