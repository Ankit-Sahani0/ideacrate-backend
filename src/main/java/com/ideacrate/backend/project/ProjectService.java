package com.ideacrate.backend.project;

import com.ideacrate.backend.user.User;
import com.ideacrate.backend.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {


    private final ProjectRepository projectRepository;
    private final ProjectContributorRepository projectContributorRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectContributorRepository projectContributorRepository, UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.projectContributorRepository = projectContributorRepository;
        this.userRepository = userRepository;
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
        newProject.setViewCount(0L);
        newProject.setAuthor(author);
        newProject.setCreatedAt(java.time.LocalDateTime.now());

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
        responseDTO.setViewCount(project.getViewCount()); // Fixed
        responseDTO.setStatus(project.getStatus());
        responseDTO.setFeedback(project.getFeedback());
        responseDTO.setGithubUrl(project.getGithubUrl());
        responseDTO.setCreatedAt(project.getCreatedAt()); // Fixed
        responseDTO.setUpdatedAt(project.getUpdatedAt());
        responseDTO.setTags(project.getTags());

        // Handle tech stack
        if (project.getTechStack() != null && !project.getTechStack().isBlank()) {
            List<String> techs = Arrays.stream(project.getTechStack().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            responseDTO.setTechStack(techs);
        } else {
            responseDTO.setTechStack(List.of());
        }

        // Handle author
        if (project.getAuthor() != null) {
            AuthorDTO authorDTO = new AuthorDTO();
            authorDTO.setId(project.getAuthor().getId());
            authorDTO.setFirstName(project.getAuthor().getFirstName());
            authorDTO.setLastName(project.getAuthor().getLastName());
            authorDTO.setProfileImageUrl(project.getAuthor().getAvatar());
            responseDTO.setAuthor(authorDTO); // Fixed
        }

        return responseDTO;
    }


    public ProjectResponseDTO incrementViewCount(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project Not Found"));

        project.setViewCount(project.getViewCount() + 1); // Fixed method name
        return mapToProjectResponseDTO(projectRepository.save(project));
    }

    public List<ContributorDTO> getProjectContributors(Long projectId){
        return projectContributorRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToContributorDTO)
                .collect(Collectors.toList());
    }

    public ContributorDTO addContributor(Long projectId, Long userId, String role, User currentUser) {
        System.out.println("=== Service addContributor Debug ===");
        System.out.println("Looking for project ID: " + projectId);
        // Check if project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        System.out.println("Project found: " + project.getTitle());
        System.out.println("Project author ID: " + project.getAuthor().getId());
        System.out.println("Current user ID: " + currentUser.getId());

        // Check if current user is the project owner
        if (!project.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only project owner can add contributors");
        }

        System.out.println("User is project owner - proceeding");

        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("User to add found: " + user.getEmail());

        // Check if user is already a contributor
        if (projectContributorRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new RuntimeException("User is already a contributor");
        }

        // Check if trying to add project owner as contributor
        if (project.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Project owner cannot be added as contributor");
        }


        // Create and save contributor
        ProjectContributor contributor = new ProjectContributor(project, user, role);
        ProjectContributor saved = projectContributorRepository.save(contributor);

        return mapToContributorDTO(saved);
    }

    public void removeContributor(Long projectId, Long userId, User currentUser) {
        // Check if project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if current user is the project owner
        if (!project.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only project owner can remove contributors");
        }

        // Check if contributor exists
        if (!projectContributorRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new RuntimeException("User is not a contributor");
        }

        projectContributorRepository.deleteByProjectIdAndUserId(projectId, userId);
    }




    private ContributorDTO mapToContributorDTO(ProjectContributor contributor) {
        ContributorDTO dto = new ContributorDTO();
        dto.setId(contributor.getId());
        dto.setUserId(contributor.getUser().getId());
        dto.setFirstName(contributor.getUser().getFirstName());
        dto.setLastName(contributor.getUser().getLastName());
        dto.setEmail(contributor.getUser().getEmail());
        dto.setProfileImageUrl(contributor.getUser().getAvatar());
        dto.setRole(contributor.getRole());
        dto.setAddedAt(contributor.getAddedAt());
        return dto;
    }

    // Add this method to your ProjectService class

    public List<ProjectResponseDTO> searchProjects(String query,
                                                   String category,
                                                   String tech,
                                                   String sortBy,
                                                   String sortDir) {

        String searchTerm = (query == null || query.isBlank()) ? null : query.trim().toLowerCase();
        String categoryTerm = (category == null || category.isBlank()
                || category.equalsIgnoreCase("All Categories")) ? null : category.trim();

        List<Project> raw = projectRepository.searchProjects(searchTerm, categoryTerm);

        // Optional tech filter (case-insensitive, matches any token)
        if (tech != null && !tech.isBlank()) {
            String techFilter = tech.trim().toLowerCase();
            raw = raw.stream()
                    .filter(p -> getTechList(p).stream()
                            .anyMatch(t -> t.equalsIgnoreCase(techFilter)))
                    .collect(Collectors.toList());
        }

        // Sorting
        Comparator<Project> comparator;
        switch (sortBy == null ? "" : sortBy) {
            case "starsCount" -> comparator = Comparator.comparing(Project::getStarsCount);
            case "viewCount" -> comparator = Comparator.comparing(Project::getViewCount);
            case "title" -> comparator = Comparator.comparing(p -> p.getTitle() == null ? "" : p.getTitle().toLowerCase());
            default -> comparator = Comparator.comparing(p -> p.getCreatedAt() == null ? java.time.LocalDateTime.MIN : p.getCreatedAt());
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        raw.sort(comparator);

        return raw.stream()
                .map(this::mapToProjectResponseDTO)
                .collect(Collectors.toList());
    }

    private List<String> getTechList(Project p) {
        if (p.getTechStack() == null || p.getTechStack().isBlank()) return List.of();
        return Arrays.stream(p.getTechStack().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }






    }