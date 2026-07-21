package com.example.demo.repository;

import com.example.demo.entity.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRecordRepository extends JpaRepository<CertificateRecord, Long> {
    Optional<CertificateRecord> findByIssuanceKey(String issuanceKey);
    Optional<CertificateRecord> findByVerificationCodeIgnoreCase(String verificationCode);
}
