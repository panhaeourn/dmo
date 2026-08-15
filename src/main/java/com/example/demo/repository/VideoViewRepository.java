package com.example.demo.repository;

import com.example.demo.entity.VideoView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select view from VideoView view where view.video.id = :videoId and view.user.id = :userId")
    Optional<VideoView> findForUpdate(@Param("videoId") Long videoId, @Param("userId") Long userId);

    Optional<VideoView> findByVideoIdAndUserId(Long videoId, Long userId);

    @Query("select coalesce(sum(view.viewCount), 0) from VideoView view where view.video.id = :videoId")
    long totalViews(@Param("videoId") Long videoId);

    @Query("select count(view) from VideoView view where view.video.id = :videoId and view.viewCount > 0")
    long uniqueViewers(@Param("videoId") Long videoId);

    @Query("""
            select view.video.id, coalesce(sum(view.viewCount), 0),
                   sum(case when view.viewCount > 0 then 1 else 0 end)
            from VideoView view
            where view.video.course.id = :courseId
            group by view.video.id
            """)
    List<Object[]> statsByCourse(@Param("courseId") Long courseId);
}
