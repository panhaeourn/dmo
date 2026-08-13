package com.example.demo.repository;

import com.example.demo.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    Optional<CourseVideo> findByFileName(String fileName);

    void deleteByCourseId(Long courseId);

    @Modifying
    @Query("update CourseVideo video set video.viewCount = coalesce(video.viewCount, 0) + 1 where video.id = :videoId")
    int incrementViewCount(@Param("videoId") Long videoId);

}
