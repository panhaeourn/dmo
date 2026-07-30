package com.example.demo.service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final AppUserRepository appUserRepository;
    private final EnrollmentRepository enrollmentRepository;

    public void requireCourseAccess(Authentication authentication, Course course) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return;
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));

        if (course == null || !enrollmentRepository.existsByUserAndCourse(user, course)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course enrollment is required");
        }
    }
}
