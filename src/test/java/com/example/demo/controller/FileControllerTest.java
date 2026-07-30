package com.example.demo.controller;

import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.service.CourseAccessService;
import com.example.demo.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest {

    private FileService fileService;
    private FileController controller;

    @BeforeEach
    void setUp() throws Exception {
        fileService = mock(FileService.class);
        CourseVideoRepository courseVideoRepository = mock(CourseVideoRepository.class);
        CourseAccessService courseAccessService = mock(CourseAccessService.class);
        controller = new FileController(fileService, courseVideoRepository, courseAccessService);
        when(fileService.metadata("lesson.mp4"))
                .thenReturn(new FileService.FileMetadata(1_000, "video/mp4", "etag-value"));
    }

    @Test
    void headReturnsMetadataWithoutOpeningVideoStream() throws Exception {
        ResponseEntity<InputStreamResource> response = controller.getFile(
                "lesson.mp4",
                null,
                HttpMethod.HEAD,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(1_000);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getETag()).isEqualTo("\"etag-value\"");
        assertThat(response.getBody()).isNull();
        verify(fileService, never()).download("lesson.mp4");
        verify(fileService, never()).downloadRange("lesson.mp4", 0, 1_000);
    }

    @Test
    void returnsRequestedByteRange() throws Exception {
        when(fileService.downloadRange("lesson.mp4", 100, 100))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        ResponseEntity<InputStreamResource> response = controller.getFile(
                "lesson.mp4",
                "bytes=100-199",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(100);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
                .isEqualTo("bytes 100-199/1000");
        verify(fileService).downloadRange("lesson.mp4", 100, 100);
    }

    @Test
    void supportsSuffixByteRange() throws Exception {
        when(fileService.downloadRange("lesson.mp4", 900, 100))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        ResponseEntity<InputStreamResource> response = controller.getFile(
                "lesson.mp4",
                "bytes=-100",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
                .isEqualTo("bytes 900-999/1000");
        verify(fileService).downloadRange("lesson.mp4", 900, 100);
    }

    @Test
    void rejectsRangeOutsideFile() throws Exception {
        ResponseEntity<InputStreamResource> response = controller.getFile(
                "lesson.mp4",
                "bytes=1000-",
                HttpMethod.GET,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes */1000");
        verify(fileService, never()).downloadRange("lesson.mp4", 1000, 1);
    }
}
