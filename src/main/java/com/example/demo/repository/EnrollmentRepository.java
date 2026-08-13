package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserAndCourse(AppUser user, Course course);

    List<Enrollment> findByUser(AppUser user);

    void deleteByCourseId(Long courseId);
}
