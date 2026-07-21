package com.example.demo.controller;

import com.example.demo.dto.CertificateIssueRequest;
import com.example.demo.dto.CertificateVerificationResponse;
import com.example.demo.service.CertificateVerificationService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CertificateVerificationController {

    private final CertificateVerificationService certificateVerificationService;

    public CertificateVerificationController(
            CertificateVerificationService certificateVerificationService
    ) {
        this.certificateVerificationService = certificateVerificationService;
    }

    @PostMapping(value = "/api/admin/certificates/issue", produces = "application/json")
    public ResponseEntity<?> issue(
            @RequestBody CertificateIssueRequest request,
            Authentication authentication
    ) {
        try {
            List<CertificateVerificationResponse> issued =
                    certificateVerificationService.issue(request, authentication);
            return ResponseEntity.ok(issued);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
        }
    }

    @PatchMapping(value = "/api/admin/certificates/{verificationCode}/revoke", produces = "application/json")
    public ResponseEntity<?> revoke(@PathVariable String verificationCode) {
        try {
            return ResponseEntity.ok(certificateVerificationService.revoke(verificationCode));
        } catch (CertificateVerificationService.CertificateNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
        }
    }

    @GetMapping(value = "/api/certificates/verify/{verificationCode}", produces = "application/json")
    public ResponseEntity<?> verify(@PathVariable String verificationCode) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(certificateVerificationService.verify(verificationCode));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
        } catch (CertificateVerificationService.CertificateNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
        }
    }

    public record ApiError(String message) {
    }
}
