package com.example.demo.controller;

import com.example.demo.dto.CourseCreateRequest;
import com.example.demo.dto.CourseResponse;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import com.example.demo.service.FileService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final AppUserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final FileService fileService;

    public CourseController(
            CourseRepository courseRepository,
            AppUserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            CourseVideoRepository courseVideoRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            FileService fileService
    ) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseVideoRepository = courseVideoRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.fileService = fileService;
    }

    @GetMapping
    public List<CourseResponse> getAll(Authentication authentication) {
        Set<Long> enrolledCourseIds = new HashSet<>();
        Map<Long, Long> purchaseCounts = paymentHistoryRepository.countPaidCoursePurchases()
                .stream()
                .collect(Collectors.toMap(
                        PaymentHistoryRepository.PaidCoursePurchaseCount::getCourseId,
                        PaymentHistoryRepository.PaidCoursePurchaseCount::getPurchaseCount
                ));

        String email = extractEmail(authentication);
        if (email != null && !email.isBlank()) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                List<Enrollment> enrollments = enrollmentRepository.findByUser(user);
                for (Enrollment e : enrollments) {
                    enrolledCourseIds.add(e.getCourse().getId());
                }
            });
        }

        return courseRepository.findAll()
                .stream()
                .map(course -> toResponse(
                        course,
                        enrolledCourseIds.contains(course.getId()),
                        purchaseCounts.getOrDefault(course.getId(), 0L)
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getOne(@PathVariable Long id, Authentication authentication) {
        AppUser user = findAuthenticatedUser(authentication);
        Course course = courseRepository.findById(id).orElse(null);
        boolean enrolled = user != null && course != null && enrollmentRepository.existsByUserAndCourse(user, course);

        final boolean finalEnrolled = enrolled;

        return course == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(toResponse(course, finalEnrolled));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @RequestBody CourseCreateRequest req,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        boolean freeAccess = Boolean.TRUE.equals(req.getFreeAccess());
        Double price = freeAccess ? 0.0 : req.getPrice();
        if (!freeAccess && (price == null || price <= 0)) price = 5.0;

        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setEmail(email);
            u.setUsername(email);
            u.setRole("USER");
            u.setPassword("GOOGLE");
            u.setName(email);
            return userRepository.save(u);
        });

        Course course = new Course();
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setPrice(price);
        course.setFreeAccess(freeAccess);
        course.setUser(user);

        Course saved = courseRepository.save(course);
        return ResponseEntity.ok(toResponse(saved, false));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseCreateRequest req,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        AppUser user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!canManageCourse(user, course)) {
            return ResponseEntity.status(403).body("Forbidden: not allowed to update this course");
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            course.setTitle(req.getTitle());
        }

        course.setDescription(req.getDescription());

        boolean freeAccess = Boolean.TRUE.equals(req.getFreeAccess());
        Double price = freeAccess ? 0.0 : req.getPrice();
        if (!freeAccess && (price == null || price <= 0)) {
            price = 5.0;
        }
        course.setPrice(price);
        course.setFreeAccess(freeAccess);

        Course saved = courseRepository.save(course);
        boolean enrolled = enrollmentRepository.existsByUserAndCourse(user, saved);

        return ResponseEntity.ok(toResponse(saved, enrolled));
    }

    @DeleteMapping("/{courseId}")
    @Transactional
    public ResponseEntity<?> deleteCourse(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        AppUser user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!canManageCourse(user, course)) {
            return ResponseEntity.status(403).body("Forbidden: not allowed to delete this course");
        }

        courseVideoRepository.deleteByCourseId(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
        return ResponseEntity.ok("Course deleted");
    }

    @PostMapping(value = "/{courseId}/video", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadCourseVideo(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws Exception {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        AppUser user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!canManageCourse(user, course)) {
            return ResponseEntity.status(403).body("Forbidden: not course owner");
        }

        String filename = fileService.upload(file);
        course.setVideoFileName(filename);

        Course saved = courseRepository.save(course);
        boolean enrolled = enrollmentRepository.existsByUserAndCourse(user, saved);

        return ResponseEntity.ok(toResponse(saved, enrolled));
    }

    @PostMapping(value = "/{courseId}/teacher-photo", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadTeacherPhoto(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "positionX", required = false) Double positionX,
            @RequestParam(value = "positionY", required = false) Double positionY,
            @RequestParam(value = "bottomDarkness", required = false) Double bottomDarkness,
            @RequestParam(value = "scale", required = false) Double scale,
            Authentication authentication
    ) throws Exception {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        AppUser user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!canManageCourse(user, course)) {
            return ResponseEntity.status(403).body("Forbidden: not allowed to update this course photo");
        }

        String filename = fileService.upload(file);
        course.setTeacherPhotoFileName(filename);
        applyTeacherPhotoLayout(course, positionX, positionY, bottomDarkness, scale);

        Course saved = courseRepository.save(course);
        boolean enrolled = enrollmentRepository.existsByUserAndCourse(user, saved);

        return ResponseEntity.ok(toResponse(saved, enrolled));
    }

    @PatchMapping("/{courseId}/teacher-photo")
    public ResponseEntity<?> updateTeacherPhotoLayout(
            @PathVariable Long courseId,
            @RequestParam(value = "positionX", required = false) Double positionX,
            @RequestParam(value = "positionY", required = false) Double positionY,
            @RequestParam(value = "bottomDarkness", required = false) Double bottomDarkness,
            @RequestParam(value = "scale", required = false) Double scale,
            Authentication authentication
    ) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        AppUser user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!canManageCourse(user, course)) {
            return ResponseEntity.status(403).body("Forbidden: not allowed to update this course photo");
        }

        applyTeacherPhotoLayout(course, positionX, positionY, bottomDarkness, scale);

        Course saved = courseRepository.save(course);
        boolean enrolled = enrollmentRepository.existsByUserAndCourse(user, saved);

        return ResponseEntity.ok(toResponse(saved, enrolled));
    }

    private String extractEmail(Authentication authentication) {
        try {
            if (authentication == null) return null;

            Object principal = authentication.getPrincipal();

            if (principal instanceof OAuth2User oAuth2User) {
                String email = oAuth2User.getAttribute("email");
                if (email != null) return email;
            }

            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }

            String name = authentication.getName();
            if ("anonymousUser".equals(name)) return null;
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    private AppUser findAuthenticatedUser(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private boolean canManageCourse(AppUser user, Course course) {
        return "ADMIN".equalsIgnoreCase(user.getRole())
                || course.getUser() != null
                && course.getUser().getEmail() != null
                && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(course.getUser().getEmail());
    }

    private void applyTeacherPhotoLayout(
            Course course,
            Double positionX,
            Double positionY,
            Double bottomDarkness,
            Double scale
    ) {
        if (positionX != null) {
            course.setTeacherPhotoPositionX(positionX);
        }
        if (positionY != null) {
            course.setTeacherPhotoPositionY(positionY);
        }
        if (bottomDarkness != null) {
            course.setTeacherPhotoBottomDarkness(bottomDarkness);
        }
        if (scale != null) {
            course.setTeacherPhotoScale(scale);
        }
    }

    private CourseResponse toResponse(Course course, boolean enrolled) {
        return toResponse(course, enrolled, 0L);
    }

    private CourseResponse toResponse(Course course, boolean enrolled, long purchaseCount) {
        String videoUrl = null;
        if (course.getVideoFileName() != null && !course.getVideoFileName().isBlank()) {
            videoUrl = "/files/" + course.getVideoFileName();
        }

        String teacherPhotoUrl = null;
        if (course.getTeacherPhotoFileName() != null && !course.getTeacherPhotoFileName().isBlank()) {
            teacherPhotoUrl = "/files/" + course.getTeacherPhotoFileName();
        }

        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.isFreeAccess(),
                course.getVideoFileName(),
                videoUrl,
                course.getTeacherPhotoFileName(),
                teacherPhotoUrl,
                course.getTeacherPhotoPositionX(),
                course.getTeacherPhotoPositionY(),
                course.getTeacherPhotoBottomDarkness(),
                course.getTeacherPhotoScale(),
                enrolled || course.isFreeAccess(),
                purchaseCount
        );
    }
}
