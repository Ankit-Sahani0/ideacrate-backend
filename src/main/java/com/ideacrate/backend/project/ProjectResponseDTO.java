package com.ideacrate.backend.project;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fullDescription; // Renamed
    private String category;
    private List<String> techStack;
    private String githubUrl;
    private String demoUrl;
    private String imageUrl;
    private int starsCount; // Renamed
    private Integer viewsCount; // Added
    private String status;
    private String feedback; // Added
    private LocalDateTime submittedAt; // Renamed
    private AuthorDTO user; // Renamed from 'author' to 'user'
    private  LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AuthorDTO> authors;
}