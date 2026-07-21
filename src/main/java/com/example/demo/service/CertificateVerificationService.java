package com.example.demo.service;

import com.example.demo.dto.CertificateIssueRequest;
import com.example.demo.dto.CertificatePublishRequest;
import com.example.demo.dto.CertificateVerificationResponse;
import com.example.demo.entity.CertificateRecord;
import com.example.demo.repository.CertificateRecordRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CertificateVerificationService {

    private static final int MAX_BATCH_SIZE = 500;

    private final CertificateRecordRepository certificateRecordRepository;

    public CertificateVerificationService(CertificateRecordRepository certificateRecordRepository) {
        this.certificateRecordRepository = certificateRecordRepository;
    }

    @Transactional
    public List<CertificateVerificationResponse> issue(
            CertificateIssueRequest request,
            Authentication authentication
    ) {
        List<CertificateIssueRequest.CertificateIssueItem> items =
                request == null ? null : request.certificates();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one certificate is required.");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("A maximum of 500 certificates can be issued at once.");
        }

        String issuedBy = authentication == null ? "unknown" : authentication.getName();
        return items.stream()
                .map(item -> issueOne(item, issuedBy))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verify(String verificationCode) {
        String normalizedCode = normalizeCode(verificationCode);
        CertificateRecord record = certificateRecordRepository
                .findByVerificationCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found."));
        if (!record.isPublished()) {
            throw new CertificateNotFoundException("Certificate not found.");
        }
        return toResponse(record);
    }

    @Transactional
    public List<CertificateVerificationResponse> publish(CertificatePublishRequest request) {
        List<String> verificationCodes = request == null ? null : request.verificationCodes();
        if (verificationCodes == null || verificationCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one certificate is required.");
        }
        if (verificationCodes.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("A maximum of 500 certificates can be published at once.");
        }

        return verificationCodes.stream()
                .map(this::publishOne)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CertificateVerificationResponse revoke(String verificationCode) {
        CertificateRecord record = certificateRecordRepository
                .findByVerificationCodeIgnoreCase(normalizeCode(verificationCode))
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found."));
        if (!record.isRevoked()) {
            record.setRevoked(true);
            record.setRevokedAt(LocalDateTime.now());
            certificateRecordRepository.save(record);
        }
        return toResponse(record);
    }

    private CertificateRecord issueOne(
            CertificateIssueRequest.CertificateIssueItem item,
            String issuedBy
    ) {
        if (item == null) {
            throw new IllegalArgumentException("Certificate data is required.");
        }

        String issuanceKey = required(item.issuanceKey(), "Issuance key", 120);
        return certificateRecordRepository.findByIssuanceKey(issuanceKey).orElseGet(() -> {
            String khmerName = clean(item.recipientNameKhmer(), 255);
            String englishName = clean(item.recipientNameEnglish(), 255);
            if (khmerName.isBlank() && englishName.isBlank()) {
                throw new IllegalArgumentException("Recipient name is required.");
            }

            String code = UUID.randomUUID().toString();
            CertificateRecord record = new CertificateRecord();
            record.setVerificationCode(code);
            record.setCertificateNumber(
                    "CITO-" + Year.now().getValue() + "-" + code.substring(0, 8).toUpperCase(Locale.ROOT)
            );
            record.setIssuanceKey(issuanceKey);
            record.setRecipientNameKhmer(khmerName);
            record.setRecipientNameEnglish(englishName);
            record.setBirthDate(required(item.birthDate(), "Birth date", 100));
            record.setCourseName(required(item.courseName(), "Course", 500));
            record.setIssueDate(required(item.issueDate(), "Issue date", 100));
            record.setIssuedByEmail(clean(issuedBy, 255));
            record.setPublished(false);
            record.setRevoked(false);
            return certificateRecordRepository.save(record);
        });
    }

    private CertificateRecord publishOne(String verificationCode) {
        CertificateRecord record = certificateRecordRepository
                .findByVerificationCodeIgnoreCase(normalizeCode(verificationCode))
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found."));
        if (!record.isPublished()) {
            record.setPublished(true);
            record.setPublishedAt(LocalDateTime.now());
            certificateRecordRepository.save(record);
        }
        return record;
    }

    private CertificateVerificationResponse toResponse(CertificateRecord record) {
        boolean valid = record.isPublished() && !record.isRevoked();
        String status = record.isRevoked() ? "REVOKED" : record.isPublished() ? "VALID" : "DRAFT";
        return new CertificateVerificationResponse(
                status,
                valid,
                record.isPublished(),
                record.getVerificationCode(),
                record.getCertificateNumber(),
                record.getRecipientNameKhmer(),
                record.getRecipientNameEnglish(),
                record.getBirthDate(),
                record.getCourseName(),
                record.getIssueDate(),
                record.getIssuedAt(),
                record.getPublishedAt(),
                record.getRevokedAt()
        );
    }

    private String normalizeCode(String value) {
        return required(value, "Verification code", 36).toLowerCase(Locale.ROOT);
    }

    private String required(String value, String label, int maxLength) {
        String cleaned = clean(value, maxLength);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return cleaned;
    }

    private String clean(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException("Certificate field is too long.");
        }
        return cleaned;
    }

    public static class CertificateNotFoundException extends RuntimeException {
        public CertificateNotFoundException(String message) {
            super(message);
        }
    }
}
