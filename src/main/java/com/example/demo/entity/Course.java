package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    private String videoFileName;
    // ✅ FK for Week 3
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;


}
