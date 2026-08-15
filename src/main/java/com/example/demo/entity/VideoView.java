package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "video_views",
        uniqueConstraints = @UniqueConstraint(name = "uq_video_views_video_user", columnNames = {"video_id", "user_id"})
)
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private CourseVideo video;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long totalWatchSeconds;

    @Column(nullable = false)
    private double progressSeconds;

    @Column(nullable = false)
    private double durationSeconds;

    @Column(nullable = false)
    private boolean completed;

    @Column(length = 36)
    private String sessionId;

    @Column(nullable = false)
    private double sessionWatchSeconds;

    @Column(nullable = false)
    private boolean sessionQualified;

    @Column(nullable = false)
    private double lastPositionSeconds;

    private Instant lastEventAt;
    private Instant lastQualifiedAt;

    public Long getId() { return id; }
    public CourseVideo getVideo() { return video; }
    public void setVideo(CourseVideo video) { this.video = video; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public long getTotalWatchSeconds() { return totalWatchSeconds; }
    public void setTotalWatchSeconds(long totalWatchSeconds) { this.totalWatchSeconds = totalWatchSeconds; }
    public double getProgressSeconds() { return progressSeconds; }
    public void setProgressSeconds(double progressSeconds) { this.progressSeconds = progressSeconds; }
    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double durationSeconds) { this.durationSeconds = durationSeconds; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public double getSessionWatchSeconds() { return sessionWatchSeconds; }
    public void setSessionWatchSeconds(double sessionWatchSeconds) { this.sessionWatchSeconds = sessionWatchSeconds; }
    public boolean isSessionQualified() { return sessionQualified; }
    public void setSessionQualified(boolean sessionQualified) { this.sessionQualified = sessionQualified; }
    public double getLastPositionSeconds() { return lastPositionSeconds; }
    public void setLastPositionSeconds(double lastPositionSeconds) { this.lastPositionSeconds = lastPositionSeconds; }
    public Instant getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }
    public Instant getLastQualifiedAt() { return lastQualifiedAt; }
    public void setLastQualifiedAt(Instant lastQualifiedAt) { this.lastQualifiedAt = lastQualifiedAt; }
}
