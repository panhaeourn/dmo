package com.example.demo.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudflareR2Config {

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.region:auto}")
    private String region;

    @Value("${cloudflare.r2.access-key}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .region(region)
                .credentials(accessKey, secretKey)
                .build();
    }
}
