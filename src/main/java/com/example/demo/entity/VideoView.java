package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_views", uniqueConstraints =
        @UniqueConstraint(columnNames = {"video_id", "user_id"}))
public class VideoView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private CourseVideo video;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private long playCount = 1;

    @Column(nullable = false)
    private LocalDateTime firstViewedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastViewedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public CourseVideo getVideo() { return video; }
    public void setVideo(CourseVideo video) { this.video = video; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }
    public LocalDateTime getFirstViewedAt() { return firstViewedAt; }
    public void setFirstViewedAt(LocalDateTime firstViewedAt) { this.firstViewedAt = firstViewedAt; }
    public LocalDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(LocalDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }
}
