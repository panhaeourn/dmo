package com.example.demo.dto;

public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private String videoFileName;
    private String videoUrl;
    private boolean enrolled;

    public CourseResponse() {}

    public CourseResponse(
            Long id,
            String title,
            String description,
            Double price,
            String videoFileName,
            String videoUrl,
            boolean enrolled
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.videoFileName = videoFileName;
        this.videoUrl = videoUrl;
        this.enrolled = enrolled;
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

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public boolean isEnrolled() { return enrolled; }
    public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }
}