package com.example.demo.service;

import com.example.demo.dto.MultipartUploadCompleteRequest;
import com.example.demo.dto.MultipartUploadStartRequest;
import com.example.demo.dto.MultipartUploadStartResponse;
import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MultipartVideoUploadService {

    public static final long PART_SIZE = 15L * 1024 * 1024;
    public static final long MAX_FILE_SIZE = 1024L * 1024 * 1024;
    private static final Duration URL_EXPIRY = Duration.ofMinutes(30);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    public MultipartUploadStartResponse start(Long courseId, MultipartUploadStartRequest request) {
        requireCourse(courseId);
        validateFile(request.fileName(), request.contentType(), request.fileSize());

        String objectKey = "course-" + courseId + "-" + UUID.randomUUID() + "_" + safeName(request.fileName());
        CreateMultipartUploadResponse created = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(normalizedContentType(request.contentType()))
                        .build()
        );

        int partCount = Math.toIntExact((request.fileSize() + PART_SIZE - 1) / PART_SIZE);
        List<String> urls = IntStream.rangeClosed(1, partCount)
                .mapToObj(part -> presignPart(objectKey, created.uploadId(), part))
                .toList();

        return new MultipartUploadStartResponse(
                created.uploadId(), objectKey, PART_SIZE, partCount,
                Math.toIntExact(URL_EXPIRY.toSeconds()), urls
        );
    }

    @Transactional
    public CourseVideo complete(Long courseId, MultipartUploadCompleteRequest request) {
        Course course = requireCourse(courseId);
        validateFile(request.fileName(), request.contentType(), request.fileSize());
        validateUploadIdentity(courseId, request.objectKey(), request.uploadId());

        int expectedParts = Math.toIntExact((request.fileSize() + PART_SIZE - 1) / PART_SIZE);
        if (request.parts() == null || request.parts().size() != expectedParts) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload parts are incomplete");
        }

        List<CompletedPart> parts = request.parts().stream()
                .sorted(Comparator.comparingInt(MultipartUploadCompleteRequest.UploadedPart::partNumber))
                .map(part -> {
                    if (part.partNumber() < 1 || part.partNumber() > expectedParts
                            || part.eTag() == null || part.eTag().isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid uploaded part");
                    }
                    return CompletedPart.builder().partNumber(part.partNumber()).eTag(part.eTag()).build();
                })
                .toList();

        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(request.objectKey())
                .uploadId(request.uploadId())
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build());

        HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket).key(request.objectKey()).build());
        if (head.contentLength() != request.fileSize()) {
            tryDelete(request.objectKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file size verification failed");
        }

        CourseVideo video = new CourseVideo();
        video.setCourse(course);
        video.setFileName(request.objectKey());
        video.setVideoUrl("/files/" + request.objectKey());
        video.setTitle(request.title() == null || request.title().isBlank() ? request.fileName() : request.title().trim());
        video.setSortOrder(0);
        try {
            return courseVideoRepository.save(video);
        } catch (RuntimeException exception) {
            tryDelete(request.objectKey());
            throw exception;
        }
    }

    public void abort(Long courseId, String objectKey, String uploadId) {
        validateUploadIdentity(courseId, objectKey, uploadId);
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucket).key(objectKey).uploadId(uploadId).build());
    }

    private String presignPart(String key, String uploadId, int partNumber) {
        UploadPartRequest uploadPart = UploadPartRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId).partNumber(partNumber).build();
        return s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                        .signatureDuration(URL_EXPIRY)
                        .uploadPartRequest(uploadPart)
                        .build())
                .url().toString();
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private void validateFile(String fileName, String contentType, long fileSize) {
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only MP4 videos are supported");
        }
        if (fileSize < 1 || fileSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video must be between 1 byte and 1 GB");
        }
        String normalized = normalizedContentType(contentType);
        if (!normalized.equals("video/mp4") && !normalized.equals("video/x-m4v")
                && !normalized.equals("application/octet-stream")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid video content type");
        }
    }

    private void validateUploadIdentity(Long courseId, String objectKey, String uploadId) {
        String prefix = "course-" + courseId + "-";
        if (objectKey == null || !objectKey.startsWith(prefix) || objectKey.contains("..")
                || uploadId == null || uploadId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload session");
        }
    }

    private String normalizedContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.toLowerCase(Locale.ROOT);
    }

    private String safeName(String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void tryDelete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ignored) {
            // Verification failure remains the primary error.
        }
    }
}
