package com.example.demo.dto;

import java.time.LocalDateTime;

public record CertificateVerificationResponse(
        String status,
        boolean valid,
        String verificationCode,
        String certificateNumber,
        String recipientNameKhmer,
        String recipientNameEnglish,
        String courseName,
        String issueDate,
        LocalDateTime issuedAt,
        LocalDateTime revokedAt
) {
}
