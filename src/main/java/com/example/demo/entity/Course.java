package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Double price = 5.0;

    private String videoFileName;

    private String teacherPhotoFileName;

    private Double teacherPhotoPositionX = 50.0;

    private Double teacherPhotoPositionY = 0.0;

    private Double teacherPhotoBottomDarkness = 90.0;

    private Double teacherPhotoScale = 1.0;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    public Course() {}

    public Course(Long id, String title, String description, Double price, String videoFileName, AppUser user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.videoFileName = videoFileName;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getVideoFileName() { return videoFileName; }
    public void setVideoFileName(String videoFileName) { this.videoFileName = videoFileName; }

    public String getTeacherPhotoFileName() { return teacherPhotoFileName; }
    public void setTeacherPhotoFileName(String teacherPhotoFileName) { this.teacherPhotoFileName = teacherPhotoFileName; }

    public Double getTeacherPhotoPositionX() { return teacherPhotoPositionX; }
    public void setTeacherPhotoPositionX(Double teacherPhotoPositionX) { this.teacherPhotoPositionX = teacherPhotoPositionX; }

    public Double getTeacherPhotoPositionY() { return teacherPhotoPositionY; }
    public void setTeacherPhotoPositionY(Double teacherPhotoPositionY) { this.teacherPhotoPositionY = teacherPhotoPositionY; }

    public Double getTeacherPhotoBottomDarkness() { return teacherPhotoBottomDarkness; }
    public void setTeacherPhotoBottomDarkness(Double teacherPhotoBottomDarkness) { this.teacherPhotoBottomDarkness = teacherPhotoBottomDarkness; }

    public Double getTeacherPhotoScale() { return teacherPhotoScale; }
    public void setTeacherPhotoScale(Double teacherPhotoScale) { this.teacherPhotoScale = teacherPhotoScale; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}
