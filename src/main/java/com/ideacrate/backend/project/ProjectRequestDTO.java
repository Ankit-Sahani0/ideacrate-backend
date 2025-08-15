package com.ideacrate.backend.project;

import lombok.Data;

@Data
public class ProjectRequestDTO {
    private String title;
    private String description;
    private String techStack;
    private String githubLink;
    private String detailedDescription;
    private String category;
    private String demoUrl;
    private String imageUrl;
}
