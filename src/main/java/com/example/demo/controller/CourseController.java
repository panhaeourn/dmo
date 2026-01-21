package com.example.demo.controller;

import com.example.demo.dto.CourseCreateRequest;
import com.example.demo.dto.CourseResponse;
import com.example.demo.entity.Course;
import com.example.demo.repository.CourseRepository;
import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepository;
    private final FileService fileService;

    @GetMapping
    public List<CourseResponse> getAll() {
        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public CourseResponse create(@RequestBody CourseCreateRequest req) {
        Course course = new Course();
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        return toResponse(courseRepository.save(course));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getOne(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(course -> ResponseEntity.ok(toResponse(course)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Attach video to course (upload to MinIO and save filename in DB)
    @PostMapping(value = "/{courseId}/video", consumes = "multipart/form-data")
    public ResponseEntity<CourseResponse> uploadCourseVideo(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        String filename = fileService.upload(file);  // returns UUID or object name
        course.setVideoFileName(filename);

        Course saved = courseRepository.save(course);
        return ResponseEntity.ok(toResponse(saved));
    }

    // ✅ helper mapper
    private CourseResponse toResponse(Course course) {
        String videoUrl = null;
        if (course.getVideoFileName() != null && !course.getVideoFileName().isBlank()) {
            videoUrl = "/files/" + course.getVideoFileName(); // adjust if your route differs
        }

        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .videoFileName(course.getVideoFileName())
                .videoUrl(videoUrl)
                .build();
    }
}
