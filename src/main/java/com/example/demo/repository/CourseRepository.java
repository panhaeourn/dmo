package com.example.demo.repository;

import com.example.demo.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("select course.videoFileName from Course course where course.videoFileName is not null and course.videoFileName <> ''")
    List<String> findAllLegacyVideoObjectKeys();

    @Query("select course.teacherPhotoFileName from Course course where course.teacherPhotoFileName is not null and course.teacherPhotoFileName <> ''")
    List<String> findAllTeacherPhotoObjectKeys();
}
