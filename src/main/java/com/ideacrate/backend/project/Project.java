package com.ideacrate.backend.project;

import com.ideacrate.backend.user.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="author_id")
    private User author;

    // Add these methods if not using Lombok @Data
    @ManyToMany
    @JoinTable(
            name="project_contributors",
            joinColumns = @JoinColumn(name="project_id"),
            inverseJoinColumns = @JoinColumn(name="user_id")
    )
    private Set<User> contributors = new HashSet<>();

    @Column(name = "view_count")
    private Long viewCount = 0L;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    
    
    @ElementCollection
    private Set<String> tags = new HashSet<>();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}