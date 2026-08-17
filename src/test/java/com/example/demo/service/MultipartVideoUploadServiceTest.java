package com.example.demo.service;

import com.example.demo.dto.MultipartUploadCompleteRequest;
import com.example.demo.dto.MultipartUploadStartRequest;
import com.example.demo.entity.Course;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartVideoUploadServiceTest {

    private MultipartVideoUploadService service;

    @BeforeEach
    void setUp() {
        CourseRepository courseRepository = mock(CourseRepository.class);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(new Course()));
        service = new MultipartVideoUploadService(
                mock(S3Client.class),
                mock(S3Presigner.class),
                courseRepository,
                mock(CourseVideoRepository.class)
        );
    }

    @Test
    void rejectsNonMp4BeforeCreatingStorageUpload() {
        assertThatThrownBy(() -> service.start(
                7L,
                new MultipartUploadStartRequest("lesson.exe", "Lesson", "application/octet-stream", 100)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsObjectKeyThatDoesNotBelongToCourse() {
        var request = new MultipartUploadCompleteRequest(
                "upload-id",
                "course-8-object.mp4",
                "lesson.mp4",
                "Lesson",
                "video/mp4",
                100,
                List.of(new MultipartUploadCompleteRequest.UploadedPart(1, "etag"))
        );

        assertThatThrownBy(() -> service.complete(7L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
