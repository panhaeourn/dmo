package com.example.demo.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient storageClient;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    public String upload(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try (InputStream is = file.getInputStream()) {
            storageClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(is, -1, 10 * 1024 * 1024)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return fileName;
    }

    public InputStream download(String fileName) throws Exception {
        return storageClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );
    }

    public InputStream downloadRange(String fileName, long offset, long length) throws Exception {
        return storageClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .offset(offset)
                        .length(length)
                        .build()
        );
    }

    public StatObjectResponse stat(String fileName) throws Exception {
        return storageClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );
    }
}
