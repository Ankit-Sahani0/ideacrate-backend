package com.ideacrate.backend.project;

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
    private String status;
    private String feedback;
    private LocalDateTime createdAt; // Use only this
    private LocalDateTime updatedAt;
    private AuthorDTO author; // Changed from 'user' back to 'author' for clarity
    private Set<String> tags;
}