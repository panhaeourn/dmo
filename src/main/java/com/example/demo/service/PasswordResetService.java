package com.example.demo.service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.PasswordResetToken;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern FIREBASE_PHONE_PATTERN =
            Pattern.compile("\"phoneNumber\"\\s*:\\s*\"([^\"]+)\"");

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.reset-password.token-expiry-minutes:15}")
    private long tokenExpiryMinutes;

    @Value("${app.reset-password.from-email:no-reply@cito-school-kh.com}")
    private String fromEmail;

    @Value("${app.reset-password.debug-return-link:false}")
    private boolean debugReturnLink;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${firebase.web-api-key:}")
    private String firebaseWebApiKey;

    @Transactional
    public ForgotPasswordResult createResetRequest(String rawEmail, String rawPhoneNumber) {
        String phoneNumber = normalizePhone(rawPhoneNumber);
        String email = normalizeEmail(rawEmail);

        passwordResetTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return createSmsResetRequest(phoneNumber);
        }

        if (email == null || email.isBlank()) {
            return ForgotPasswordResult.generic(null);
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ForgotPasswordResult.generic(null);
        }

        String registeredPhone = normalizePhone(user.getPhoneNumber());
        if (registeredPhone != null && !registeredPhone.isBlank()) {
            return createSmsResetRequest(user, true);
        }

        passwordResetTokenRepository.deleteAllByEmailIgnoreCase(email);

        String rawToken = generateRawToken();
        String resetLink = buildResetLink(rawToken);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(tokenExpiryMinutes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(email);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setRequestedAt(now);
        resetToken.setExpiresAt(expiresAt);
        passwordResetTokenRepository.save(resetToken);

        sendResetEmail(user, resetLink, expiresAt);

        String debugLink = debugReturnLink ? resetLink : null;
        return ForgotPasswordResult.generic(debugLink);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Reset token is required.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or has already been used."));

        if (resetToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Reset link has already been used.");
        }

        if (resetToken.getExpiresAt() == null || LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Reset link has expired. Please request a new one.");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeEmail(resetToken.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Account no longer exists."));

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteAllByEmailIgnoreCase(user.getEmail());
    }

    @Transactional
    public void resetPasswordByFirebaseVerification(
            String rawEmail,
            String rawPhoneNumber,
            String rawFirebaseIdToken,
            String newPassword
    ) {
        String email = normalizeEmail(rawEmail);
        String phoneNumber = normalizePhone(rawPhoneNumber);
        String firebaseIdToken = rawFirebaseIdToken == null ? "" : rawFirebaseIdToken.trim();

        if ((email == null || email.isBlank()) && (phoneNumber == null || phoneNumber.isBlank())) {
            throw new IllegalArgumentException("Email or phone number is required.");
        }

        if (firebaseIdToken.isBlank()) {
            throw new IllegalArgumentException("Firebase verification token is required.");
        }

        AppUser user = resolveUserForSmsReset(email, phoneNumber);
        String registeredPhone = normalizePhone(user.getPhoneNumber());
        if (registeredPhone == null || registeredPhone.isBlank()) {
            throw new IllegalArgumentException("This account does not have a registered phone number.");
        }

        String verifiedPhone = normalizePhone(lookupFirebasePhoneNumber(firebaseIdToken));
        if (verifiedPhone == null || !verifiedPhone.equals(registeredPhone)) {
            throw new IllegalArgumentException("Verified phone number does not match this account.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        passwordResetTokenRepository.deleteAllByEmailIgnoreCase(user.getEmail());
    }

    private ForgotPasswordResult createSmsResetRequest(String phoneNumber) {
        AppUser user = appUserRepository.findByPhoneNumber(phoneNumber).orElse(null);
        if (user == null) {
            return isFirebasePhoneConfigured()
                    ? ForgotPasswordResult.firebase(maskPhone(phoneNumber), null, false)
                    : ForgotPasswordResult.generic(null);
        }

        return createSmsResetRequest(user, false);
    }

    private ForgotPasswordResult createSmsResetRequest(AppUser user, boolean requestedByEmail) {
        String phoneNumber = normalizePhone(user.getPhoneNumber());
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return requestedByEmail
                    ? ForgotPasswordResult.generic(null)
                    : ForgotPasswordResult.generic(null);
        }

        if (isFirebasePhoneConfigured()) {
            return ForgotPasswordResult.firebase(
                    maskPhone(phoneNumber),
                    phoneNumber,
                    requestedByEmail
            );
        }

        return requestedByEmail
                ? ForgotPasswordResult.generic(null)
                : ForgotPasswordResult.generic(null);
    }

    private AppUser resolveUserForSmsReset(String email, String phoneNumber) {
        if (email != null && !email.isBlank()) {
            return appUserRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Email is not registered."));
        }

        return appUserRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Phone number is not registered."));
    }

    private void sendResetEmail(AppUser user, String resetLink, LocalDateTime expiresAt) {
        String displayName = user.getName() == null || user.getName().isBlank()
                ? user.getEmail()
                : user.getName();

        String subject = "Reset your CITO password";
        String body = String.format(
                "Hello %s,%n%n" +
                        "We received a request to reset your password.%n%n" +
                        "Use this link to choose a new password:%n%s%n%n" +
                        "This link expires at %s.%n%n" +
                        "If you did not request this, you can safely ignore this email.%n",
                displayName,
                resetLink,
                expiresAt.format(EXPIRY_FORMAT)
        );

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            log.info("Password reset link for {}: {}", user.getEmail(), resetLink);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Unable to send password reset email to {}. Reset link: {}", user.getEmail(), resetLink, ex);
        }
    }

    private boolean isFirebasePhoneConfigured() {
        return firebaseWebApiKey != null && !firebaseWebApiKey.isBlank();
    }

    private String lookupFirebasePhoneNumber(String firebaseIdToken) {
        if (!isFirebasePhoneConfigured()) {
            throw new IllegalStateException("Firebase phone verification is not configured.");
        }

        try {
            String requestBody = "{\"idToken\":\"" + escapeJson(firebaseIdToken) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:lookup?key="
                            + urlEncode(firebaseWebApiKey)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Unable to validate Firebase phone token. Status: {} body: {}",
                        response.statusCode(), response.body());
                throw new IllegalArgumentException("Firebase phone verification is invalid or expired.");
            }

            String body = response.body() == null ? "" : response.body();
            Matcher matcher = FIREBASE_PHONE_PATTERN.matcher(body);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Firebase phone verification is invalid or expired.");
            }
            return matcher.group(1);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Unable to validate Firebase phone token", ex);
            throw new IllegalStateException("Unable to validate Firebase phone verification right now.");
        }
    }

    private String buildResetLink(String rawToken) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + "/#/reset-password" + separator +
                "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String normalized = phoneNumber.replaceAll("[^\\d+]", "");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.startsWith("0")) {
            return "+855" + normalized.substring(1);
        }
        if (normalized.startsWith("855")) {
            return "+855" + normalized.substring(3);
        }
        if (!normalized.startsWith("+") && normalized.matches("\\d+")) {
            return "+" + normalized;
        }
        return normalized;
    }

    private String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "";
        }
        String visible = phoneNumber.length() <= 4
                ? phoneNumber
                : phoneNumber.substring(phoneNumber.length() - 4);
        return "******" + visible;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record ForgotPasswordResult(
            String message,
            String debugResetUrl,
            String maskedPhoneNumber,
            String channel,
            String resolvedPhoneNumber
    ) {
        public static ForgotPasswordResult generic(String debugResetUrl) {
            return new ForgotPasswordResult(
                    "If an account with that email exists, a reset link has been prepared.",
                    debugResetUrl,
                    null,
                    "EMAIL",
                    null
            );
        }

        public static ForgotPasswordResult firebase(
                String maskedPhoneNumber,
                String resolvedPhoneNumber,
                boolean requestedByEmail
        ) {
            return new ForgotPasswordResult(
                    requestedByEmail
                            ? "If an account with that email exists, the registered phone number is ready for Firebase OTP verification."
                            : "If an account with that phone number exists, it is ready for Firebase OTP verification.",
                    null,
                    maskedPhoneNumber,
                    "FIREBASE",
                    resolvedPhoneNumber
            );
        }
    }
}
