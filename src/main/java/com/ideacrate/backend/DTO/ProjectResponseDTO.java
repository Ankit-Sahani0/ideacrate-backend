package com.ideacrate.backend.DTO;

import com.ideacrate.backend.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class ProjectResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fullDescription;
    private String category;
    private List<String> techStack;
    private String githubUrl;
    private String demoUrl;
    private String imageUrl;
    private int starsCount;
    private Long viewCount; // Changed to Long
    private ProjectStatus status;
    private String adminComment;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt; // Use only this
    private LocalDateTime updatedAt;

    // Primary (leader) author, preserved for backward compatibility with existing frontend code
    private AuthorDTO author;

    // All authors (leader + members) for this project, used by admin and group views
    private List<AuthorDTO> authors;

    // For /projects/my: role of the currently authenticated user on this project (LEADER or MEMBER)
    private String currentUserRole;

    private Set<String> tags;

    // --- AI augmentation fields ---
    // Clean structured summary extracted from the project's description.
    private String aiSummaryProblem;
    private String aiSummarySolution;
    private String aiSummaryTechnologies;
    private String aiSummaryImpact;

    // AI-suggested tags (technologies/frameworks/domains), already merged
    // with any manually supplied tech stack and de-duplicated.
    private List<String> aiTags;
}