package com.example.demo.repository;

import com.example.demo.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByEmailIgnoreCase(String email);

    void deleteAllByExpiresAtBefore(LocalDateTime cutoff);
}
