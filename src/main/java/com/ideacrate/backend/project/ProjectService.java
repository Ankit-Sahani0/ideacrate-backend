package com.ideacrate.backend.project;

import com.ideacrate.backend.user.User;
import com.ideacrate.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public ProjectService(UserRepository userRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
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

    public ProjectResponseDTO createProject(ProjectRequestDTO request, User author) {
        if (request == null || author == null) {
            throw new IllegalArgumentException("Request and author cannot be null");
        }

        Project newProject = new Project();
        newProject.setTitle(request.getTitle());
        newProject.setDescription(request.getDescription());
        newProject.setFullDescription(request.getDetailedDescription());
        newProject.setCategory(request.getCategory());
        newProject.setDemoUrl(request.getDemoUrl());
        newProject.setImageUrl(request.getImageUrl());

        // Handle tech stack properly
        if (request.getTechStack() != null && !request.getTechStack().isEmpty()) {
            newProject.setTechStack(String.join(",", request.getTechStack()));
        }

        newProject.setGithubUrl(request.getGithubLink());
        newProject.setFeedback(request.getFeedback());
        newProject.setStatus("PENDING");
        newProject.setStarsCount(0);
        newProject.setViewsCount(0);
        newProject.setAuthor(author);
        newProject.setSubmittedAt(java.time.LocalDateTime.now());

        Project savedProject = projectRepository.save(newProject);
        return mapToProjectResponseDTO(savedProject);
    }

    // --- PUT Endpoint ---
    public Optional<ProjectResponseDTO> updateProject(Long id, ProjectRequestDTO requestDTO) {
        if (id == null || requestDTO == null) {
            throw new IllegalArgumentException("Project ID and request cannot be null");
        }

        return projectRepository.findById(id)
                .map(existingProject -> {
                    existingProject.setTitle(requestDTO.getTitle());
                    existingProject.setDescription(requestDTO.getDescription());
                    existingProject.setFullDescription(requestDTO.getDetailedDescription());
                    existingProject.setCategory(requestDTO.getCategory());

                    // Handle tech stack properly
                    if (requestDTO.getTechStack() != null && !requestDTO.getTechStack().isEmpty()) {
                        existingProject.setTechStack(String.join(",", requestDTO.getTechStack()));
                    }

                    existingProject.setGithubUrl(requestDTO.getGithubLink());
                    existingProject.setDemoUrl(requestDTO.getDemoUrl());
                    existingProject.setImageUrl(requestDTO.getImageUrl());
                    existingProject.setUpdatedAt(java.time.LocalDateTime.now());

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
    public Optional<ProjectResponseDTO> likeProject(Long id, User currentUser) {
        return projectRepository.findById(id)
                .map(projectToLike -> {
                    // You can add logic here to track who liked the project if needed
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

        if (project.getTechStack() != null && !project.getTechStack().isBlank()) {
            List<String> techs = Arrays.stream(project.getTechStack().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            responseDTO.setTechStack(techs);
        } else {
            responseDTO.setTechStack(List.of());
        }

        if (project.getAuthor() != null) {
            AuthorDTO a = new AuthorDTO();
            a.setId(project.getAuthor().getId());
            a.setFirstName(project.getAuthor().getFirstName());
            a.setLastName(project.getAuthor().getLastName());
            a.setProfileImageUrl(project.getAuthor().getAvatar());
            responseDTO.setUser(a);
        }

        return responseDTO;
    }

    public void addContributor(Long projectId, Long userId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Only project author can add contributors
        if (!project.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only project author can add contributors");
        }

        User contributor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is already a contributor
        if (project.getContributors().contains(contributor)) {
            throw new RuntimeException("User is already a contributor");
        }

        project.getContributors().add(contributor);
        projectRepository.save(project);
    }

    public void removeContributor(Long projectId, Long userId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Only project author can remove contributors
        if (!project.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only project author can remove contributors");
        }

        User contributor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        project.getContributors().remove(contributor);
        projectRepository.save(project);
    }

    public List<AuthorDTO> getProjectContributors(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return project.getContributors().stream()
                .map(user -> {
                    AuthorDTO authorDTO = new AuthorDTO();
                    authorDTO.setId(user.getId());
                    authorDTO.setFirstName(user.getFirstName());
                    authorDTO.setLastName(user.getLastName());
                    authorDTO.setProfileImageUrl(user.getAvatar());
                    return authorDTO;
                })
                .toList();
    }

    public boolean isContributor(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return project.getContributors().stream()
                .anyMatch(contributor -> contributor.getId().equals(userId));
    }

    public ProjectResponseDTO incrementViewCount(Long projectId) {
        Project project = projectRepository.findById(projectId)
        .orElseThrow( ()-> new RuntimeException("Project Not Found") );
                
        project.setViewCount(project.getViewCount() + 1);
        return mapToProjectResponseDTO(projectRepository.save(project));
    }

}