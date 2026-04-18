package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"})
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    public Enrollment() {
    }

    public Enrollment(AppUser user, Course course) {
        this.user = user;
        this.course = course;
        this.enrolledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}