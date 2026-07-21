package com.example.demo.service;

import com.example.demo.dto.CertificateIssueRequest;
import com.example.demo.dto.CertificatePublishRequest;
import com.example.demo.dto.CertificateVerificationResponse;
import com.example.demo.entity.CertificateRecord;
import com.example.demo.repository.CertificateRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificateVerificationServiceTest {

    private CertificateRecordRepository repository;
    private CertificateVerificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CertificateRecordRepository.class);
        service = new CertificateVerificationService(repository);
    }

    @Test
    void issuesAnOfficialCertificateWithoutPrivateSpreadsheetFields() {
        when(repository.findByIssuanceKey("batch-1")).thenReturn(Optional.empty());
        when(repository.save(any(CertificateRecord.class))).thenAnswer(invocation -> {
            CertificateRecord record = invocation.getArgument(0);
            record.setIssuedAt(LocalDateTime.of(2026, 7, 21, 10, 0));
            return record;
        });

        List<CertificateVerificationResponse> result = service.issue(
                request("batch-1"),
                new UsernamePasswordAuthenticationToken("admin@cito.study", "")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("DRAFT");
        assertThat(result.get(0).valid()).isFalse();
        assertThat(result.get(0).published()).isFalse();
        assertThat(result.get(0).certificateNumber()).startsWith("CITO-2026-");
        assertThat(result.get(0).recipientNameEnglish()).isEqualTo("Sok Dara");
        assertThat(result.get(0).birthDate()).isEqualTo("15 August 2005");
        assertThat(result.get(0).courseName()).isEqualTo("Microsoft Excel");
    }

    @Test
    void reusesTheExistingRecordForTheSameIssuanceKey() {
        CertificateRecord existing = record(false);
        when(repository.findByIssuanceKey("batch-1")).thenReturn(Optional.of(existing));

        List<CertificateVerificationResponse> result = service.issue(
                request("batch-1"),
                new UsernamePasswordAuthenticationToken("admin@cito.study", "")
        );

        assertThat(result.get(0).verificationCode()).isEqualTo(existing.getVerificationCode());
        verify(repository, never()).save(any(CertificateRecord.class));
    }

    @Test
    void reportsRevokedCertificatesAsInvalid() {
        CertificateRecord revoked = record(true);
        when(repository.findByVerificationCodeIgnoreCase("abc-123")).thenReturn(Optional.of(revoked));

        CertificateVerificationResponse result = service.verify("ABC-123");

        assertThat(result.status()).isEqualTo("REVOKED");
        assertThat(result.valid()).isFalse();
        assertThat(result.revokedAt()).isNotNull();
    }

    @Test
    void draftCertificateIsHiddenFromPublicVerification() {
        CertificateRecord draft = record(false);
        draft.setPublished(false);
        when(repository.findByVerificationCodeIgnoreCase("abc-123")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.verify("ABC-123"))
                .isInstanceOf(CertificateVerificationService.CertificateNotFoundException.class);
    }

    @Test
    void publishesDraftCertificateToThePublicRegistry() {
        CertificateRecord draft = record(false);
        draft.setPublished(false);
        when(repository.findByVerificationCodeIgnoreCase("abc-123")).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        List<CertificateVerificationResponse> result = service.publish(
                new CertificatePublishRequest(List.of("ABC-123"))
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("VALID");
        assertThat(result.get(0).valid()).isTrue();
        assertThat(result.get(0).published()).isTrue();
        assertThat(result.get(0).publishedAt()).isNotNull();
        verify(repository).save(draft);
    }

    private CertificateIssueRequest request(String issuanceKey) {
        return new CertificateIssueRequest(List.of(new CertificateIssueRequest.CertificateIssueItem(
                issuanceKey,
                "សុខ ដារ៉ា",
                "Sok Dara",
                "15 August 2005",
                "Microsoft Excel",
                "21 July 2026"
        )));
    }

    private CertificateRecord record(boolean revoked) {
        CertificateRecord record = new CertificateRecord();
        record.setVerificationCode("abc-123");
        record.setCertificateNumber("CITO-2026-ABC12345");
        record.setIssuanceKey("batch-1");
        record.setRecipientNameKhmer("សុខ ដារ៉ា");
        record.setRecipientNameEnglish("Sok Dara");
        record.setBirthDate("15 August 2005");
        record.setCourseName("Microsoft Excel");
        record.setIssueDate("21 July 2026");
        record.setIssuedAt(LocalDateTime.of(2026, 7, 21, 10, 0));
        record.setIssuedByEmail("admin@cito.study");
        record.setPublished(true);
        record.setPublishedAt(LocalDateTime.of(2026, 7, 21, 10, 5));
        record.setRevoked(revoked);
        if (revoked) {
            record.setRevokedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        }
        return record;
    }
}
