package com.ideacrate.backend.DTO;

import lombok.Data;

import java.util.List;

@Data
public class ProjectRequestDTO {
    private String title;
    private String description;
    private String techStack;
    private String githubUrl;
    private String detailedDescription;
    private String category;
    private String demoUrl;
    private String imageUrl;

    // Optional list of additional author user IDs (excluding the leader/current user)
    private List<Long> authorIds;
}
