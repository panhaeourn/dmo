package com.example.demo.controller;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.VideoViewRepository;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.VideoView;
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

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/course-videos")
@RequiredArgsConstructor
@Slf4j
public class CourseVideoController {

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final FileService fileService;
    private final CourseAccessService courseAccessService;
    private final AppUserRepository appUserRepository;
    private final VideoViewRepository videoViewRepository;

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

        videoViewRepository.deleteByVideoId(videoId);
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
    public ResponseEntity<Void> recordView(@PathVariable Long videoId, Authentication authentication) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        courseAccessService.requireCourseAccess(authentication, video.getCourse());

        AppUser user = appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.noContent().build();
        }

        LocalDateTime now = LocalDateTime.now();
        VideoView view = videoViewRepository.findByVideoAndUser(video, user).orElseGet(() -> {
            VideoView created = new VideoView();
            created.setVideo(video);
            created.setUser(user);
            created.setFirstViewedAt(now);
            created.setPlayCount(0);
            return created;
        });
        view.setPlayCount(view.getPlayCount() + 1);
        view.setLastViewedAt(now);
        videoViewRepository.save(view);
        return ResponseEntity.noContent().build();
    }
}
