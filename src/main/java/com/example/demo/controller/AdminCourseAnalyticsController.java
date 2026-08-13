package com.example.demo.controller;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.VideoView;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseAnalyticsController {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final VideoViewRepository videoViewRepository;

    @GetMapping("/{courseId}/analytics")
    @Transactional(readOnly = true)
    public CourseAnalytics analytics(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(courseId);
        Map<Long, List<VideoView>> viewsByVideo = videoViewRepository
                .findByVideoCourseIdOrderByLastViewedAtDesc(courseId).stream()
                .collect(Collectors.groupingBy(view -> view.getVideo().getId()));

        List<StudentItem> students = enrollments.stream().map(e -> new StudentItem(
                e.getUser().getId(), e.getUser().getName(), e.getUser().getEmail(), e.getEnrolledAt()
        )).toList();
        List<VideoItem> videos = courseVideoRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId).stream()
                .map(video -> videoItem(video, viewsByVideo.getOrDefault(video.getId(), List.of())))
                .toList();
        return new CourseAnalytics(course.getId(), course.getTitle(), students.size(), students, videos);
    }

    private VideoItem videoItem(CourseVideo video, List<VideoView> views) {
        long totalPlays = views.stream().mapToLong(VideoView::getPlayCount).sum();
        List<ViewerItem> viewers = views.stream().map(view -> new ViewerItem(
                view.getUser().getId(), view.getUser().getName(), view.getUser().getEmail(),
                view.getPlayCount(), view.getFirstViewedAt(), view.getLastViewedAt()
        )).toList();
        return new VideoItem(video.getId(), video.getTitle(), totalPlays, viewers.size(), viewers);
    }

    public record CourseAnalytics(Long courseId, String courseTitle, int enrollmentCount,
                                  List<StudentItem> students, List<VideoItem> videos) {}
    public record StudentItem(Long userId, String name, String email, LocalDateTime enrolledAt) {}
    public record VideoItem(Long videoId, String title, long totalPlays, int uniqueViewers,
                            List<ViewerItem> viewers) {}
    public record ViewerItem(Long userId, String name, String email, long playCount,
                             LocalDateTime firstViewedAt, LocalDateTime lastViewedAt) {}
}
