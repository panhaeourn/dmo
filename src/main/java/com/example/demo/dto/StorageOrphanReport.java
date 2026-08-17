package com.example.demo.dto;

import java.time.Instant;
import java.util.List;

public record StorageOrphanReport(
        Instant generatedAt,
        long scannedObjects,
        long referencedObjects,
        long orphanBytes,
        List<OrphanObject> orphans
) {
    public record OrphanObject(String objectKey, long size, Instant lastModified) {}
}
