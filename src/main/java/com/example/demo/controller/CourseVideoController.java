package com.example.demo.controller;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.dto.VideoHeartbeatRequest;
import com.example.demo.dto.VideoViewResponse;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.service.CourseAccessService;
import com.example.demo.service.FileService;
import com.example.demo.service.VideoViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/course-videos")
@RequiredArgsConstructor
@Slf4j
public class CourseVideoController {

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final FileService fileService;
    private final CourseAccessService courseAccessService;
    private final VideoViewService videoViewService;

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

    @PostMapping("/{videoId}/view-heartbeat")
    public VideoViewResponse recordViewHeartbeat(
            @PathVariable Long videoId,
            @RequestBody VideoHeartbeatRequest request,
            Authentication authentication
    ) {
        return videoViewService.heartbeat(videoId, request, authentication);
    }

    @GetMapping("/{videoId}/view-stats")
    public VideoViewResponse getViewStats(@PathVariable Long videoId, Authentication authentication) {
        return videoViewService.stats(videoId, authentication);
    }
}
