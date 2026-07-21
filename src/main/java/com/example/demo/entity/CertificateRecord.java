package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_records")
public class CertificateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_code", nullable = false, unique = true, length = 36)
    private String verificationCode;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 40)
    private String certificateNumber;

    @Column(name = "issuance_key", nullable = false, unique = true, length = 120)
    private String issuanceKey;

    @Column(name = "recipient_name_khmer", length = 255)
    private String recipientNameKhmer;

    @Column(name = "recipient_name_english", length = 255)
    private String recipientNameEnglish;

    @Column(name = "course_name", nullable = false, length = 500)
    private String courseName;

    @Column(name = "issue_date", nullable = false, length = 100)
    private String issueDate;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "issued_by_email", nullable = false, length = 255)
    private String issuedByEmail;

    // The database default preserves certificates created before draft publishing was introduced.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean published;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    void prePersist() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }
    public String getIssuanceKey() { return issuanceKey; }
    public void setIssuanceKey(String issuanceKey) { this.issuanceKey = issuanceKey; }
    public String getRecipientNameKhmer() { return recipientNameKhmer; }
    public void setRecipientNameKhmer(String recipientNameKhmer) { this.recipientNameKhmer = recipientNameKhmer; }
    public String getRecipientNameEnglish() { return recipientNameEnglish; }
    public void setRecipientNameEnglish(String recipientNameEnglish) { this.recipientNameEnglish = recipientNameEnglish; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public String getIssuedByEmail() { return issuedByEmail; }
    public void setIssuedByEmail(String issuedByEmail) { this.issuedByEmail = issuedByEmail; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
