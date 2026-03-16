package com.ideacrate.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ideacrate.backend.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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

    // Demo links can exceed 255 chars
    @Column(columnDefinition = "TEXT")
    private String demoUrl;

    // Image links can exceed 255 chars
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private int starsCount = 0;

    @Column(nullable = false)
    private Long viewCount = 0L; // Use only this one

    // AI-generated structured summary for the project, stored as JSON text.
    // Expected shape:
    // { "summary": { "problem": "...", "solution": "...", "technologies": "...", "impact": "..." },
    //   "tags": ["Python", "Machine Learning", ...] }
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    // AI-extracted tags (technologies/frameworks/domains), stored as JSONB in Postgres.
    // Persisted as a JSON array of strings, e.g. ["Machine Learning", "Python", "Healthcare"].
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_tags", columnDefinition = "jsonb")
    private List<String> aiTags;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.SUBMITTED;

    @Column(columnDefinition = "TEXT")
    private String adminComment;

    @Column
    private Long reviewedBy;

    @Column
    private LocalDateTime reviewedAt;

    // Can be a comma-separated string; can exceed 255 chars in real usage
    @Column(nullable = false, columnDefinition = "TEXT")
    private String techStack;

    // URLs can exceed 255 chars (e.g. long repo URLs, query params)
    @Column(nullable = false, columnDefinition = "TEXT")
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

        // Tracks which users have liked this project so that
        // each user can only like once and can toggle their like.
        @ManyToMany
        @JoinTable(
            name = "project_likes",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
        )
        @JsonIgnore
        private Set<User> likedByUsers = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void ensureNonNullDefaults() {
        if (viewCount == null) viewCount = 0L;
        // starsCount is primitive (int) so it can’t be null, but keep future-proofing here.
        if (status == null) status = ProjectStatus.SUBMITTED;
        if (techStack == null) techStack = "";
        if (githubUrl == null) githubUrl = "";
        if (category == null) category = "";
    }
}