package com.example.demo.payway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class PayWayCrypto {

    public static String sha512Base64(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
