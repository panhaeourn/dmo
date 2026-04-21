package com.example.demo.repository;

import com.example.demo.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    void deleteByCourseId(Long courseId);

}
