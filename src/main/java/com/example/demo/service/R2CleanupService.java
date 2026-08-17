package com.example.demo.service;

import com.example.demo.entity.R2CleanupJob;
import com.example.demo.repository.R2CleanupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2CleanupService {
    private final R2CleanupJobRepository cleanupJobRepository;
    private final FileService fileService;

    @Transactional
    public void enqueue(String objectKey, String reason) {
        if (objectKey == null || objectKey.isBlank()) return;
        cleanupJobRepository.findByObjectKey(objectKey).orElseGet(() -> {
            R2CleanupJob job = new R2CleanupJob();
            job.setObjectKey(objectKey);
            job.setReason(reason == null || reason.isBlank() ? "unspecified" : reason);
            return cleanupJobRepository.save(job);
        });
    }

    @Scheduled(fixedDelayString = "${app.r2-cleanup.interval-ms:60000}")
    @Transactional
    public void processDueJobs() {
        for (R2CleanupJob job : cleanupJobRepository.findTop20ByNextAttemptAtLessThanEqualOrderByIdAsc(Instant.now())) {
            try {
                fileService.delete(job.getObjectKey());
                cleanupJobRepository.delete(job);
                log.info("Deleted queued R2 object {} ({})", job.getObjectKey(), job.getReason());
            } catch (Exception exception) {
                int attempts = job.getAttempts() + 1;
                long delayMinutes = Math.min(1440, 1L << Math.min(attempts, 10));
                job.setAttempts(attempts);
                job.setNextAttemptAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
                job.setLastError(truncate(exception.getMessage()));
                cleanupJobRepository.save(job);
                log.warn("R2 cleanup failed for {}; retry {} scheduled", job.getObjectKey(), attempts);
            }
        }
    }

    private String truncate(String value) {
        if (value == null) return "Unknown storage error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
