package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.CourseVideo;
import com.example.demo.entity.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
    Optional<VideoView> findByVideoAndUser(CourseVideo video, AppUser user);
    List<VideoView> findByVideoCourseIdOrderByLastViewedAtDesc(Long courseId);
    void deleteByVideoCourseId(Long courseId);
    void deleteByVideoId(Long videoId);
}
