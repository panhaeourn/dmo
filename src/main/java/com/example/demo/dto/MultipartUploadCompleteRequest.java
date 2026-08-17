package com.example.demo.dto;

import java.util.List;

public record MultipartUploadCompleteRequest(
        String uploadId,
        String objectKey,
        String fileName,
        String title,
        String contentType,
        long fileSize,
        List<UploadedPart> parts
) {
    public record UploadedPart(int partNumber, String eTag) {}
}
