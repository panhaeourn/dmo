package com.example.demo.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaEncryptor {

    /**
     * Read PEM text from either:
     * - classpath:keys/payway_public.pem
     * - a normal filesystem path (C:\... or /opt/...)
     */
    public static String readPem(String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalArgumentException("filePath is null/blank");
            }

            if (filePath.startsWith("classpath:")) {
                String cp = filePath.replace("classpath:", "").replaceFirst("^/+", "");
                ClassPathResource resource = new ClassPathResource(cp);
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }

            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read RSA public key file: " + filePath, e);
        }
    }

    /**
     * Encrypt plainText using RSA public key in PEM format and return Base64 ciphertext.
     *
     * Uses RSA/ECB/PKCS1Padding which is the typical requirement for payment gateways.
     */
    public static String encryptBase64(String plainText, String publicKeyPem) {
        try {
            if (plainText == null) plainText = "";
            if (publicKeyPem == null || publicKeyPem.isBlank()) {
                throw new IllegalArgumentException("publicKeyPem is null/blank");
            }

            PublicKey publicKey = parsePublicKeyFromPem(publicKeyPem);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encrypt failed", e);
        }
    }

    private static PublicKey parsePublicKeyFromPem(String publicKeyPem) throws Exception {
        // Remove PEM header/footer + whitespace/newlines
        String clean = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}