package com.example.demo.dto;

import java.util.List;

public record MultipartUploadStartResponse(
        String uploadId,
        String objectKey,
        long partSize,
        int partCount,
        int expiresInSeconds,
        List<String> partUrls
) {
}
