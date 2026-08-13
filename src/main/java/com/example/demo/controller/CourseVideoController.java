package com.example.demo.controller;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.service.CourseAccessService;
import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/course-videos")
@RequiredArgsConstructor
@Slf4j
public class CourseVideoController {

    private static final Duration VIEW_COOLDOWN = Duration.ofMinutes(30);
    private final Map<String, Instant> recentViews = new ConcurrentHashMap<>();

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final FileService fileService;
    private final CourseAccessService courseAccessService;

    @PostMapping("/{courseId}/upload")
    public CourseVideo uploadVideo(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) throws Exception {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        String fileName = fileService.upload(file);

        CourseVideo video = new CourseVideo();
        video.setCourse(course);
        video.setFileName(fileName);
        video.setVideoUrl("/files/" + fileName);

        if (title != null && !title.isBlank()) {
            video.setTitle(title);
        } else {
            video.setTitle(file.getOriginalFilename());
        }

        video.setSortOrder(0);

        return courseVideoRepository.save(video);
    }

    @GetMapping("/course/{courseId}")
    public List<CourseVideo> getVideosByCourse(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        courseAccessService.requireCourseAccess(authentication, course);
        return courseVideoRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long videoId) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        courseVideoRepository.delete(video);

        String fileName = video.getFileName();
        if (fileName != null && !fileName.isBlank()) {
            try {
                fileService.delete(fileName);
            } catch (Exception exception) {
                log.warn("Deleted lesson {} but could not remove R2 object {}", videoId, fileName, exception);
            }
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{videoId}/view")
    @Transactional
    public Map<String, Long> recordView(@PathVariable Long videoId, Authentication authentication) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        courseAccessService.requireCourseAccess(authentication, video.getCourse());

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin) {
            String key = authentication.getName().toLowerCase() + ":" + videoId;
            Instant now = Instant.now();
            Instant previous = recentViews.get(key);
            if (previous == null || Duration.between(previous, now).compareTo(VIEW_COOLDOWN) >= 0) {
                courseVideoRepository.incrementViewCount(videoId);
                recentViews.put(key, now);
                video.setViewCount(video.getViewCount() + 1);
            }
        }

        return Map.of("viewCount", video.getViewCount());
    }
}
