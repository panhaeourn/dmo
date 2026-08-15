package com.example.demo.service;

import com.example.demo.dto.VideoHeartbeatRequest;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.entity.VideoView;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.VideoViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VideoViewServiceTest {

    private CourseVideoRepository courseVideoRepository;
    private VideoViewRepository videoViewRepository;
    private VideoViewService service;
    private AppUser user;
    private CourseVideo video;

    @BeforeEach
    void setUp() {
        courseVideoRepository = mock(CourseVideoRepository.class);
        videoViewRepository = mock(VideoViewRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        CourseAccessService courseAccessService = mock(CourseAccessService.class);
        service = spy(new VideoViewService(
                courseVideoRepository,
                mock(CourseRepository.class),
                videoViewRepository,
                appUserRepository,
                courseAccessService
        ));

        user = new AppUser();
        user.setId(9L);
        user.setEmail("student@example.com");
        video = new CourseVideo();
        video.setCourse(new Course());

        when(courseVideoRepository.findById(4L)).thenReturn(Optional.of(video));
        when(appUserRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void countsOneQualifiedViewAndDoesNotCountTheSameSessionTwice() {
        String sessionId = UUID.randomUUID().toString();
        Instant started = Instant.parse("2026-08-15T10:00:00Z");
        VideoView view = existingView(sessionId, started);
        when(videoViewRepository.findForUpdate(4L, 9L)).thenReturn(Optional.of(view));
        when(videoViewRepository.totalViews(4L)).thenReturn(1L);
        when(videoViewRepository.uniqueViewers(4L)).thenReturn(1L);
        doReturn(started.plusSeconds(31), started.plusSeconds(25_200)).when(service).currentTime();

        var authentication = new UsernamePasswordAuthenticationToken("student@example.com", "", List.of());
        var first = service.heartbeat(4L, new VideoHeartbeatRequest(sessionId, 31, 120, true), authentication);
        var second = service.heartbeat(4L, new VideoHeartbeatRequest(sessionId, 60, 120, true), authentication);

        assertThat(first.viewCounted()).isTrue();
        assertThat(second.viewCounted()).isFalse();
        assertThat(view.getViewCount()).isEqualTo(1);
        assertThat(view.isSessionQualified()).isTrue();
    }

    @Test
    void seekingToTheEndDoesNotMarkTheLessonComplete() {
        String sessionId = UUID.randomUUID().toString();
        Instant started = Instant.parse("2026-08-15T10:00:00Z");
        VideoView view = existingView(sessionId, started);
        when(videoViewRepository.findForUpdate(4L, 9L)).thenReturn(Optional.of(view));
        doReturn(started.plusSeconds(10)).when(service).currentTime();

        var authentication = new UsernamePasswordAuthenticationToken("student@example.com", "", List.of());
        var response = service.heartbeat(
                4L,
                new VideoHeartbeatRequest(sessionId, 119, 120, true),
                authentication
        );

        assertThat(response.completed()).isFalse();
        assertThat(view.getProgressSeconds()).isLessThan(12);
    }

    private VideoView existingView(String sessionId, Instant started) {
        VideoView view = new VideoView();
        view.setVideo(video);
        view.setUser(user);
        view.setSessionId(sessionId);
        view.setLastEventAt(started);
        view.setLastPositionSeconds(0);
        return view;
    }
}
