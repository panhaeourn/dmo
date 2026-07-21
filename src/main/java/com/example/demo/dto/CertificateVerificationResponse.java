package com.example.demo.dto;

import java.time.LocalDateTime;

public record CertificateVerificationResponse(
        String status,
        boolean valid,
        boolean published,
        String verificationCode,
        String certificateNumber,
        String recipientNameKhmer,
        String recipientNameEnglish,
        String courseName,
        String issueDate,
        LocalDateTime issuedAt,
        LocalDateTime publishedAt,
        LocalDateTime revokedAt
) {
}
