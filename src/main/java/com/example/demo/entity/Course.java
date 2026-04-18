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

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}