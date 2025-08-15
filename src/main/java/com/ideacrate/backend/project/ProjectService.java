package com.ideacrate.backend.project;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // --- GET Endpoints ---
    public List<ProjectResponseDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToProjectResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProjectResponseDTO> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::mapToProjectResponseDTO);
    }

    // --- POST Endpoint ---
    // In ProjectService.java

    public ProjectResponseDTO createProject(ProjectRequestDTO requestDTO) {
        Project newProject = new Project();

        newProject.setTitle(requestDTO.getTitle());
        newProject.setDescription(requestDTO.getDescription());
        newProject.setFullDescription(requestDTO.getDetailedDescription());
        newProject.setCategory(requestDTO.getCategory());
        newProject.setTechStack(requestDTO.getTechStack());
        newProject.setDemoUrl(requestDTO.getDemoUrl());
        newProject.setImageUrl(requestDTO.getImageUrl());

        // --- THIS IS THE MISSING LINE ---
        // The DTO has 'githubLink', but the entity has 'githubUrl'. We need to map it.
        newProject.setGithubUrl(requestDTO.getGithubLink());

        newProject.setStatus("PENDING");
        newProject.setStarsCount(0);
        newProject.setViewsCount(0);

        Project savedProject = projectRepository.save(newProject);
        return mapToProjectResponseDTO(savedProject);
    }

    // --- PUT Endpoint ---
    public Optional<ProjectResponseDTO> updateProject(Long id, ProjectRequestDTO requestDTO) {
        return projectRepository.findById(id)
                .map(existingProject -> {
                    existingProject.setTitle(requestDTO.getTitle());
                    existingProject.setDescription(requestDTO.getDescription());
                    existingProject.setFullDescription(requestDTO.getDetailedDescription());
                    existingProject.setCategory(requestDTO.getCategory());
                    existingProject.setTechStack(requestDTO.getTechStack());
                    existingProject.setGithubUrl(requestDTO.getGithubLink());
                    existingProject.setDemoUrl(requestDTO.getDemoUrl());
                    existingProject.setImageUrl(requestDTO.getImageUrl());

                    Project updatedProject = projectRepository.save(existingProject);
                    return mapToProjectResponseDTO(updatedProject);
                });
    }

    // --- DELETE Endpoint ---
    public boolean deleteProject(Long id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // --- LIKE Endpoint ---
    public Optional<ProjectResponseDTO> likeProject(Long id) {
        return projectRepository.findById(id)
                .map(projectToLike -> {
                    projectToLike.setStarsCount(projectToLike.getStarsCount() + 1);
                    Project updatedProject = projectRepository.save(projectToLike);
                    return mapToProjectResponseDTO(updatedProject);
                });
    }


    // --- Private Helper Methods ---
    private ProjectResponseDTO mapToProjectResponseDTO(Project project) {
        ProjectResponseDTO responseDTO = new ProjectResponseDTO();
        responseDTO.setId(project.getId());
        responseDTO.setTitle(project.getTitle());
        responseDTO.setDescription(project.getDescription());
        responseDTO.setFullDescription(project.getFullDescription());
        responseDTO.setCategory(project.getCategory());
        responseDTO.setDemoUrl(project.getDemoUrl());
        responseDTO.setImageUrl(project.getImageUrl());
        responseDTO.setStarsCount(project.getStarsCount());
        responseDTO.setViewsCount(project.getViewsCount());
        responseDTO.setStatus(project.getStatus());
        responseDTO.setFeedback(project.getFeedback());
        responseDTO.setGithubUrl(project.getGithubUrl());
        responseDTO.setSubmittedAt(project.getSubmittedAt());

        if (project.getTechStack() != null && !project.getTechStack().isEmpty()) {
            responseDTO.setTechStack(Arrays.asList(project.getTechStack().split(",")));
        } else {
            responseDTO.setTechStack(List.of()); // Ensure it's never null
        }

        responseDTO.setUser(createPlaceholderAuthor());
        return responseDTO;
    }

    private AuthorDTO createPlaceholderAuthor() {
        AuthorDTO authorDTO = new AuthorDTO();
        // In a real app, you would fetch the project's actual author
        authorDTO.setId(1L);
        authorDTO.setName("Placeholder User");
        authorDTO.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=placeholder");
        authorDTO.setUniversity("Placeholder University");
        return authorDTO;
    }
}