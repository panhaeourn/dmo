package com.example.demo.repository;

import com.example.demo.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    Optional<CourseVideo> findByFileName(String fileName);

    void deleteByCourseId(Long courseId);

}
