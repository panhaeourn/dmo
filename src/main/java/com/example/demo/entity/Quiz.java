package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String question;

    // ✅ FK: quizzes.course_id -> courses.id
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
