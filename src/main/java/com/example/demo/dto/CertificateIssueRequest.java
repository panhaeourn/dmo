package com.example.demo.dto;

import java.util.List;

public record CertificateIssueRequest(List<CertificateIssueItem> certificates) {
    public record CertificateIssueItem(
            String issuanceKey,
            String recipientNameKhmer,
            String recipientNameEnglish,
            String courseName,
            String issueDate
    ) {
    }
}
