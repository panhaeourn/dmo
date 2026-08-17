package com.example.demo.dto;

public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private boolean freeAccess;
    private String videoFileName;
    private String videoUrl;
    private String teacherPhotoFileName;
    private String teacherPhotoUrl;
    private Double teacherPhotoPositionX;
    private Double teacherPhotoPositionY;
    private Double teacherPhotoBottomDarkness;
    private Double teacherPhotoScale;
    private boolean enrolled;
    private long purchaseCount;

    public CourseResponse() {}

    public CourseResponse(
            Long id,
            String title,
            String description,
            Double price,
            boolean freeAccess,
            String videoFileName,
            String videoUrl,
            String teacherPhotoFileName,
            String teacherPhotoUrl,
            Double teacherPhotoPositionX,
            Double teacherPhotoPositionY,
            Double teacherPhotoBottomDarkness,
            Double teacherPhotoScale,
            boolean enrolled,
            long purchaseCount
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.freeAccess = freeAccess;
        this.videoFileName = videoFileName;
        this.videoUrl = videoUrl;
        this.teacherPhotoFileName = teacherPhotoFileName;
        this.teacherPhotoUrl = teacherPhotoUrl;
        this.teacherPhotoPositionX = teacherPhotoPositionX;
        this.teacherPhotoPositionY = teacherPhotoPositionY;
        this.teacherPhotoBottomDarkness = teacherPhotoBottomDarkness;
        this.teacherPhotoScale = teacherPhotoScale;
        this.enrolled = enrolled;
        this.purchaseCount = purchaseCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public boolean isFreeAccess() { return freeAccess; }
    public void setFreeAccess(boolean freeAccess) { this.freeAccess = freeAccess; }

    public String getVideoFileName() { return videoFileName; }
    public void setVideoFileName(String videoFileName) { this.videoFileName = videoFileName; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getTeacherPhotoFileName() { return teacherPhotoFileName; }
    public void setTeacherPhotoFileName(String teacherPhotoFileName) { this.teacherPhotoFileName = teacherPhotoFileName; }

    public String getTeacherPhotoUrl() { return teacherPhotoUrl; }
    public void setTeacherPhotoUrl(String teacherPhotoUrl) { this.teacherPhotoUrl = teacherPhotoUrl; }

    public Double getTeacherPhotoPositionX() { return teacherPhotoPositionX; }
    public void setTeacherPhotoPositionX(Double teacherPhotoPositionX) { this.teacherPhotoPositionX = teacherPhotoPositionX; }

    public Double getTeacherPhotoPositionY() { return teacherPhotoPositionY; }
    public void setTeacherPhotoPositionY(Double teacherPhotoPositionY) { this.teacherPhotoPositionY = teacherPhotoPositionY; }

    public Double getTeacherPhotoBottomDarkness() { return teacherPhotoBottomDarkness; }
    public void setTeacherPhotoBottomDarkness(Double teacherPhotoBottomDarkness) { this.teacherPhotoBottomDarkness = teacherPhotoBottomDarkness; }

    public Double getTeacherPhotoScale() { return teacherPhotoScale; }
    public void setTeacherPhotoScale(Double teacherPhotoScale) { this.teacherPhotoScale = teacherPhotoScale; }

    public boolean isEnrolled() { return enrolled; }
    public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }

    public long getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(long purchaseCount) { this.purchaseCount = purchaseCount; }
}
