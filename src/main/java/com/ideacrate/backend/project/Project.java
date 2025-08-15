package com.ideacrate.backend.project;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String fullDescription; // Renamed from detailedDescription

    @Column(nullable = false)
    private String category;

    @Column
    private String demoUrl;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private int starsCount = 0; // Renamed from likes

    @Column
    private Integer viewsCount = 0; // New field

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String feedback; // New field

    @Column(nullable = false)
    private String techStack;

    @Column(nullable = false)
    private String githubUrl; // Renamed from githubLink

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime submittedAt; // Renamed from createdAt

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}