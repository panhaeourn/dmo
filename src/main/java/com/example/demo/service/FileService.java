package com.example.demo.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient storageClient;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    private final Map<String, FileMetadata> metadataCache = new ConcurrentHashMap<>();

    public String upload(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try (InputStream is = file.getInputStream()) {
            ObjectWriteResponse response = storageClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(is, -1, 10 * 1024 * 1024)
                            .contentType(file.getContentType())
                            .build()
            );

            metadataCache.put(
                    fileName,
                    new FileMetadata(file.getSize(), file.getContentType(), response.etag())
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

    public FileMetadata metadata(String fileName) throws Exception {
        FileMetadata cached = metadataCache.get(fileName);
        if (cached != null) {
            return cached;
        }

        StatObjectResponse stat = storageClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );

        FileMetadata metadata = new FileMetadata(stat.size(), stat.contentType(), stat.etag());
        metadataCache.put(fileName, metadata);
        return metadata;
    }

    public record FileMetadata(long size, String contentType, String etag) {
    }
}
