package com.example.demo.controller;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/course-videos")
@RequiredArgsConstructor
public class CourseVideoController {

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final FileService fileService;

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
    public List<CourseVideo> getVideosByCourse(@PathVariable Long courseId) {
        return courseVideoRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
    }
}