package com.example.demo.auth;

import com.example.demo.entity.AppUser;
import com.example.demo.jwt.JwtService;
import com.example.demo.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {

        if (request.email == null || request.email.isBlank()
                || request.password == null || request.password.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Email and password are required"));
        }

        if (userRepository.findByEmail(request.email).isPresent()) {
            return ResponseEntity.badRequest().body(new ApiError("Email already exists"));
        }

        AppUser user = new AppUser();
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole("USER");
        userRepository.save(user);

        return ResponseEntity.ok(new ApiMessage("User registered"));
    }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            var authToken = new UsernamePasswordAuthenticationToken(request.email, request.password);
            authenticationManager.authenticate(authToken);

            String token = jwtService.generateToken(request.email);
            return ResponseEntity.ok(new TokenResponse(token));

        } catch (BadCredentialsException ex) {
            // ✅ return 401 instead of letting it become 403
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Invalid email or password"));
        }
    }

    // ✅ Protected endpoint (requires JWT)
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<?> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Unauthorized"));
        }

        String email = authentication.getName();

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("User not found"));
        }

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // ===== DTOs (kept in the SAME FILE) =====

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class TokenResponse {
        public String token;
        public TokenResponse(String token) { this.token = token; }
    }

    public static class ApiError {
        public String message;
        public ApiError(String message) { this.message = message; }
    }

    public static class ApiMessage {
        public String message;
        public ApiMessage(String message) { this.message = message; }
    }
}
