package com.example.demo.repository;

import com.example.demo.entity.R2CleanupJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface R2CleanupJobRepository extends JpaRepository<R2CleanupJob, Long> {
    Optional<R2CleanupJob> findByObjectKey(String objectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<R2CleanupJob> findTop20ByNextAttemptAtLessThanEqualOrderByIdAsc(Instant now);
}
