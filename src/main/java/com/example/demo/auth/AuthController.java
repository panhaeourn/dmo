package com.example.demo.auth;

import com.example.demo.dto.ClaimReceptionistRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.ReceptionistClaimCode;
import com.example.demo.jwt.JwtService;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ReceptionistClaimCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ReceptionistClaimCodeRepository receptionistClaimCodeRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-emails:}")
    private String adminEmails;

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (request.username == null || request.username.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Username is required"));
        }

        if (request.email == null || request.email.isBlank()
                || request.phoneNumber == null || request.phoneNumber.isBlank()
                || request.password == null || request.password.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Email, phone number, and password are required"));
        }

        String email = request.email.trim().toLowerCase();
        String phoneNumber = normalizePhone(request.phoneNumber);

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.badRequest().body(new ApiError("Email already exists"));
        }

        AppUser user = new AppUser();
        user.setUsername(request.username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setName(request.username == null || request.username.isBlank() ? email : request.username.trim());
        user.setRole(isConfiguredAdmin(email) ? "ADMIN" : "USER");

        userRepository.save(user);

        return ResponseEntity.ok(new ApiMessage("User registered"));
    }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse
    ) {
        try {
            String email = normalizeEmail(request.email);
            var authToken = new UsernamePasswordAuthenticationToken(email, request.password);
            authenticationManager.authenticate(authToken);

            promoteAdminIfConfigured(email);

            String token = jwtService.generateToken(email);
            addAccessTokenCookie(httpRequest, httpResponse, token, 60 * 60);
            return ResponseEntity.ok(new TokenResponse(token));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Invalid email or password"));
        }
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String normalized = phoneNumber.replaceAll("[^\\d+]", "");
        if (normalized.startsWith("0")) {
            return "+855" + normalized.substring(1);
        }
        if (!normalized.startsWith("+") && normalized.matches("\\d+")) {
            return "+" + normalized;
        }
        return normalized;
    }

    @PostMapping(value = "/logout", produces = "application/json")
    public ResponseEntity<?> logout(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        try {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            String cookie =
                    "access_token=;" +
                            " Path=/;" +
                            " Max-Age=0;" +
                            " HttpOnly;" +
                            ("localhost".equalsIgnoreCase(request.getServerName())
                                    ? " SameSite=Lax;"
                                    : " Secure; SameSite=None;");

            response.addHeader("Set-Cookie", cookie);

            return ResponseEntity.ok(new ApiMessage("Logged out"));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiMessage("Logged out"));
        }
    }

    @PostMapping(value = "/claim-receptionist", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> claimReceptionist(
            @RequestBody ClaimReceptionistRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Unauthorized"));
        }

        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Code is required"));
        }

        String email = extractEmail(authentication);
        System.out.println("claimReceptionist email = " + email);

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Cannot determine user email"));
        }

        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);

        if (user == null) {
            System.out.println("claimReceptionist user not found for email = " + email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("User not found"));
        }

        if (syncAdminRoleIfConfigured(user) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setPassword(null);
            return ResponseEntity.ok(user);
        }

        ReceptionistClaimCode claimCode = receptionistClaimCodeRepository.findByCode(request.getCode().trim())
                .orElse(null);

        if (claimCode == null) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid receptionist code"));
        }

        if (claimCode.isUsed()) {
            return ResponseEntity.badRequest().body(new ApiError("Code already used"));
        }

        if (claimCode.getExpiresAt() == null || LocalDateTime.now().isAfter(claimCode.getExpiresAt())) {
            return ResponseEntity.badRequest().body(new ApiError("Code expired"));
        }

        if (claimCode.getTargetEmail() == null || !claimCode.getTargetEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(new ApiError("This code is not assigned to your account"));
        }

        user.setRole("RECEPTIONIST");
        userRepository.save(user);

        claimCode.setUsed(true);
        claimCode.setUsedAt(LocalDateTime.now());
        receptionistClaimCodeRepository.save(claimCode);

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<?> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Unauthorized"));
        }

        String email = extractEmail(authentication);
        System.out.println("me email = " + email);

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Cannot determine user email"));
        }

        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);

        if (user == null) {
            System.out.println("me user not found for email = " + email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("User not found"));
        }

        syncAdminRoleIfConfigured(user);

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            Object emailAttr = oauth2User.getAttributes().get("email");
            if (emailAttr != null) {
                return String.valueOf(emailAttr);
            }
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return authentication.getName();
    }

    private void addAccessTokenCookie(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            String token,
            int maxAgeSeconds
    ) {
        boolean isLocal = "localhost".equalsIgnoreCase(request.getServerName());

        String setCookie =
                "access_token=" + token +
                        "; Path=/" +
                        "; Max-Age=" + maxAgeSeconds +
                        "; HttpOnly" +
                        (isLocal ? "" : "; Secure") +
                        "; SameSite=" + (isLocal ? "Lax" : "None");

        response.addHeader("Set-Cookie", setCookie);
    }

    private void promoteAdminIfConfigured(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .ifPresent(this::syncAdminRoleIfConfigured);
    }

    private boolean syncAdminRoleIfConfigured(AppUser user) {
        if (user == null || !isConfiguredAdmin(user.getEmail())) {
            return false;
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ADMIN");
            userRepository.save(user);
        }
        return true;
    }

    private boolean isConfiguredAdmin(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }
        return configuredAdminEmails().contains(normalizedEmail);
    }

    private Set<String> configuredAdminEmails() {
        if (adminEmails == null || adminEmails.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(adminEmails.split(","))
                .map(this::normalizeEmail)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class TokenResponse {
        public String token;
        public TokenResponse(String token) {
            this.token = token;
        }
    }

    public static class ApiError {
        public String message;
        public ApiError(String message) {
            this.message = message;
        }
    }

    public static class ApiMessage {
        public String message;
        public ApiMessage(String message) {
            this.message = message;
        }
    }
}





