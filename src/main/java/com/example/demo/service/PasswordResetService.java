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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    @Transactional
    public ForgotPasswordResult createResetRequest(String rawEmail) {
        String email = normalizeEmail(rawEmail);

        passwordResetTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());

        if (email == null || email.isBlank()) {
            return ForgotPasswordResult.generic(null);
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ForgotPasswordResult.generic(null);
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

    public record ForgotPasswordResult(String message, String debugResetUrl) {
        public static ForgotPasswordResult generic(String debugResetUrl) {
            return new ForgotPasswordResult(
                    "If an account with that email exists, a reset link has been prepared.",
                    debugResetUrl
            );
        }
    }
}
