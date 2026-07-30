package com.example.demo.service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseAccessServiceTest {

    private AppUserRepository appUserRepository;
    private EnrollmentRepository enrollmentRepository;
    private CourseAccessService service;
    private Course course;
    private AppUser user;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        service = new CourseAccessService(appUserRepository, enrollmentRepository);
        course = new Course();
        user = new AppUser();
        user.setEmail("student@example.com");
    }

    @Test
    void rejectsAnonymousAccess() {
        assertThatThrownBy(() -> service.requireCourseAccess(null, course))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void permitsEnrolledStudent() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "student@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(appUserRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(user));
        when(enrollmentRepository.existsByUserAndCourse(user, course)).thenReturn(true);

        assertThatCode(() -> service.requireCourseAccess(authentication, course))
                .doesNotThrowAnyException();
        verify(enrollmentRepository).existsByUserAndCourse(user, course);
    }

    @Test
    void rejectsStudentWithoutEnrollment() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "student@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(appUserRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.requireCourseAccess(authentication, course))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void permitsAdminWithoutEnrollment() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThatCode(() -> service.requireCourseAccess(authentication, course))
                .doesNotThrowAnyException();
    }
}
