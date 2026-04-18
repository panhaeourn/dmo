package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.ReceptionistClaimCode;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ReceptionistClaimCodeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/receptionist-codes")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminReceptionistController {

    private final ReceptionistClaimCodeRepository receptionistClaimCodeRepository;
    private final AppUserRepository appUserRepository;

    public AdminReceptionistController(
            ReceptionistClaimCodeRepository receptionistClaimCodeRepository,
            AppUserRepository appUserRepository
    ) {
        this.receptionistClaimCodeRepository = receptionistClaimCodeRepository;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    public Map<String, Object> generateCode(@RequestBody Map<String, String> body, Authentication authentication) {
        String targetEmail = body.get("email");

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        String code = "REC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        LocalDateTime now = LocalDateTime.now();

        ReceptionistClaimCode claimCode = new ReceptionistClaimCode();
        claimCode.setCode(code);
        claimCode.setTargetEmail(targetEmail.trim());
        claimCode.setCreatedAt(now);
        claimCode.setExpiresAt(now.plusDays(1));
        claimCode.setUsed(false);
        claimCode.setCreatedByAdminEmail(authentication != null ? authentication.getName() : null);

        receptionistClaimCodeRepository.save(claimCode);

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("code", claimCode.getCode());
        out.put("targetEmail", claimCode.getTargetEmail());
        out.put("createdAt", claimCode.getCreatedAt());
        out.put("expiresAt", claimCode.getExpiresAt());
        out.put("used", claimCode.isUsed());
        return out;
    }

    @GetMapping
    public List<ReceptionistClaimCode> getAllCodes() {
        return receptionistClaimCodeRepository.findAllByOrderByIdDesc();
    }

    @GetMapping("/users")
    public List<AppUser> getReceptionists() {
        return appUserRepository.findAll().stream()
                .filter(user -> "RECEPTIONIST".equalsIgnoreCase(user.getRole()))
                .peek(user -> user.setPassword(null))
                .toList();
    }

    @PatchMapping("/remove/{userId}")
    public Map<String, Object> removeReceptionist(@PathVariable Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole("USER");
        appUserRepository.save(user);

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("message", "Receptionist role removed successfully");
        out.put("userId", user.getId());
        out.put("email", user.getEmail());
        out.put("role", user.getRole());
        return out;
    }
}