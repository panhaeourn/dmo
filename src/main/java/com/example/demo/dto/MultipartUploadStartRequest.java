package com.example.demo.dto;

public record MultipartUploadStartRequest(
        String fileName,
        String title,
        String contentType,
        long fileSize
) {
}
