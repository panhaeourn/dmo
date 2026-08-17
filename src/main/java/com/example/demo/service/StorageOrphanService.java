package com.example.demo.service;

import com.example.demo.dto.StorageOrphanReport;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StorageOrphanService {
    private static final int MINIMUM_AGE_HOURS = 24;
    private static final int MAX_QUEUE_SIZE = 500;

    private final FileService fileService;
    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final R2CleanupService cleanupService;

    public StorageOrphanReport report(int requestedMinimumAgeHours) throws Exception {
        int minimumAgeHours = Math.max(MINIMUM_AGE_HOURS, requestedMinimumAgeHours);
        Set<String> referenced = referencedKeys();
        List<FileService.StoredObject> stored = fileService.listObjects();
        Instant cutoff = Instant.now().minus(Duration.ofHours(minimumAgeHours));

        List<StorageOrphanReport.OrphanObject> orphans = stored.stream()
                .filter(object -> object.lastModified() != null && object.lastModified().isBefore(cutoff))
                .filter(object -> !referenced.contains(object.key()))
                .map(object -> new StorageOrphanReport.OrphanObject(object.key(), object.size(), object.lastModified()))
                .toList();
        long bytes = orphans.stream().mapToLong(StorageOrphanReport.OrphanObject::size).sum();

        return new StorageOrphanReport(Instant.now(), stored.size(), referenced.size(), bytes, orphans);
    }

    public int queue(List<String> requestedKeys, int requestedMinimumAgeHours) throws Exception {
        if (requestedKeys == null || requestedKeys.isEmpty() || requestedKeys.size() > MAX_QUEUE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose between 1 and 500 orphan objects");
        }

        Set<String> requested = new HashSet<>(requestedKeys);
        if (requested.size() != requestedKeys.size() || requested.stream().anyMatch(this::invalidKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or duplicate object key");
        }

        Set<String> confirmedOrphans = report(requestedMinimumAgeHours).orphans().stream()
                .map(StorageOrphanReport.OrphanObject::objectKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!confirmedOrphans.containsAll(requested)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more objects are active, recent, or no longer orphaned");
        }

        requested.forEach(key -> cleanupService.enqueue(key, "confirmed-orphan"));
        return requested.size();
    }

    private Set<String> referencedKeys() {
        Set<String> keys = new HashSet<>(courseVideoRepository.findAllStoredObjectKeys());
        keys.addAll(courseRepository.findAllLegacyVideoObjectKeys());
        keys.addAll(courseRepository.findAllTeacherPhotoObjectKeys());
        keys.removeIf(key -> key == null || key.isBlank());
        return keys;
    }

    private boolean invalidKey(String key) {
        return key == null || key.isBlank() || key.length() > 1024 || key.contains("..") || key.startsWith("/");
    }
}
