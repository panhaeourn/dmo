package com.example.demo.controller;

import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.service.CourseAccessService;
import com.example.demo.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseVideoControllerTest {

    private CourseVideoRepository courseVideoRepository;
    private FileService fileService;
    private CourseVideoController controller;

    @BeforeEach
    void setUp() {
        CourseRepository courseRepository = mock(CourseRepository.class);
        courseVideoRepository = mock(CourseVideoRepository.class);
        fileService = mock(FileService.class);
        CourseAccessService courseAccessService = mock(CourseAccessService.class);
        controller = new CourseVideoController(
                courseRepository,
                courseVideoRepository,
                fileService,
                courseAccessService
        );
    }

    @Test
    void deletesLessonAndItsStoredVideo() throws Exception {
        CourseVideo video = new CourseVideo();
        video.setFileName("lesson.mp4");
        when(courseVideoRepository.findById(7L)).thenReturn(Optional.of(video));

        ResponseEntity<Void> response = controller.deleteVideo(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courseVideoRepository).delete(video);
        verify(fileService).delete("lesson.mp4");
    }

    @Test
    void returnsNotFoundForMissingLesson() {
        when(courseVideoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.deleteVideo(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
