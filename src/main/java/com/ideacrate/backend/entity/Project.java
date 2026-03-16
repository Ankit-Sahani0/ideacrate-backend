package com.ideacrate.backend.project;

import com.ideacrate.backend.enums.ProjectStatus;
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
    private String fullDescription;

    @Column(nullable = false)
    private String category;

    @Column
    private String demoUrl;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private int starsCount = 0;

    @Column(nullable = false)
    private Long viewCount = 0L; // Use only this one

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false)
    private String techStack;

    @Column(nullable = false)
    private String githubUrl;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // Use only this one

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="author_id", nullable = false)
    private User author;

    @ElementCollection
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
}