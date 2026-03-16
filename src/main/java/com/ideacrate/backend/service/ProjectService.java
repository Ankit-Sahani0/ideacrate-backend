package com.ideacrate.backend.service;

import com.ideacrate.backend.DTO.AuthorDTO;
import com.ideacrate.backend.DTO.ContributorDTO;
import com.ideacrate.backend.DTO.ProjectRequestDTO;
import com.ideacrate.backend.DTO.ProjectResponseDTO;
import com.ideacrate.backend.entity.Project;
import com.ideacrate.backend.entity.ProjectContributor;
import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.enums.ProjectStatus;
import com.ideacrate.backend.enums.Role;
import com.ideacrate.backend.repository.ProjectContributorRepository;
import com.ideacrate.backend.repository.ProjectRepository;
import com.ideacrate.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {


    private final ProjectRepository projectRepository;
    private final ProjectContributorRepository projectContributorRepository;
    private final UserRepository userRepository;
    private final AIProjectService aiProjectService;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectContributorRepository projectContributorRepository,
                          UserRepository userRepository,
                          AIProjectService aiProjectService,
                          ObjectMapper objectMapper) {

        this.projectRepository = projectRepository;
        this.projectContributorRepository = projectContributorRepository;
        this.userRepository = userRepository;
        this.aiProjectService = aiProjectService;
        this.objectMapper = objectMapper;
    }

    // --- GET Endpoints ---
    public List<ProjectResponseDTO> getAllProjects() {
        // Uses JOIN FETCH to load author in the same SQL query → eliminates N+1.
        return projectRepository.findAllWithAuthor().stream()
            .filter(p -> p.getStatus() == ProjectStatus.APPROVED) // Only return approved projects to public
                .map(this::mapToProjectResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProjectResponseDTO> getAllProjectsForAdmin() {
        return projectRepository.findAllWithAuthor().stream()
                .map(this::mapToProjectResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProjectResponseDTO> getPendingProjects() {
        // Admin only functionality to see pending queue
        return projectRepository.findAllWithAuthorByStatusIn(List.of(ProjectStatus.SUBMITTED, ProjectStatus.PENDING))
            .stream()
            .map(this::mapToProjectResponseDTO)
            .collect(Collectors.toList());
    }

        public List<ProjectResponseDTO> getReviewedProjects() {
        return projectRepository.findAllWithAuthorByStatusIn(List.of(ProjectStatus.APPROVED, ProjectStatus.REJECTED))
            .stream()
            .map(this::mapToProjectResponseDTO)
            .collect(Collectors.toList());
        }

    public Optional<ProjectResponseDTO> getProjectById(Long id) {
        // Uses JOIN FETCH so the author is never lazy-loaded separately.
        return projectRepository.findByIdWithAuthor(id)
                .map(this::mapToProjectResponseDTO);
    }


    // --- POST Endpoint ---
    // In ProjectService.java

    public ProjectResponseDTO createProject(ProjectRequestDTO request, User author) {
        if (request == null || author == null) {
            throw new IllegalArgumentException("Request and author cannot be null");
        }

        // --- Validate and resolve additional authors ---
        List<Long> requestedAuthorIds = request.getAuthorIds() != null
                ? new ArrayList<>(request.getAuthorIds())
                : new ArrayList<>();

        // Remove duplicates and the current user if included
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(requestedAuthorIds);
        uniqueIds.remove(author.getId());

        if (uniqueIds.size() > 3) {
            throw new IllegalArgumentException("A project can have at most 3 additional authors (4 total including leader).");
        }

        List<User> additionalAuthors = List.of();
        if (!uniqueIds.isEmpty()) {
            additionalAuthors = userRepository.findAllById(uniqueIds);

            if (additionalAuthors.size() != uniqueIds.size()) {
                throw new IllegalArgumentException("One or more selected authors do not exist.");
            }

            boolean anyNonStudent = additionalAuthors.stream()
                    .anyMatch(u -> u.getRole() != Role.STUDENT);
            if (anyNonStudent) {
                throw new IllegalArgumentException("Only students can be project authors.");
            }
        }

        Project newProject = new Project();
        newProject.setTitle(request.getTitle());
        newProject.setDescription(request.getDescription());
        newProject.setFullDescription(request.getDetailedDescription());
        newProject.setCategory(request.getCategory());
        newProject.setDemoUrl(request.getDemoUrl());
        newProject.setImageUrl(request.getImageUrl());

        // Handle tech stack properly
        if (request.getTechStack() != null && !request.getTechStack().isBlank()) {
            newProject.setTechStack(request.getTechStack().trim());
        } else {
            newProject.setTechStack("");
        }

        newProject.setGithubUrl(request.getGithubUrl());
        newProject.setStatus(ProjectStatus.SUBMITTED);
        newProject.setStarsCount(0);
        newProject.setViewCount(0L);
        // Current user is always the leader/owner
        newProject.setAuthor(author);

        // Attempt AI enrichment, but NEVER let failures block project creation.
        try {
            aiProjectService.enrichProjectWithAI(newProject, request);
        } catch (Exception ignored) {
            // Intentionally swallow: submission must succeed even if AI is misconfigured.
        }

        Project savedProject = projectRepository.save(newProject);

        // Create author records: leader + additional members
        // Leader
        ProjectContributor leaderContributor = new ProjectContributor(savedProject, author, "LEADER");
        projectContributorRepository.save(leaderContributor);

        // Members
        for (User member : additionalAuthors) {
            ProjectContributor contributor = new ProjectContributor(savedProject, member, "MEMBER");
            projectContributorRepository.save(contributor);
        }

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
                    if (requestDTO.getTechStack() != null && !requestDTO.getTechStack().isBlank()) {
                        existingProject.setTechStack(requestDTO.getTechStack().trim());
                    }

                    existingProject.setGithubUrl(requestDTO.getGithubUrl());
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
                    // Toggle like for the current user based on user id
                    // so each user can only like a project once.
                    boolean alreadyLiked = projectToLike.getLikedByUsers().stream()
                            .anyMatch(u -> u.getId().equals(currentUser.getId()));

                    if (alreadyLiked) {
                        projectToLike.getLikedByUsers().removeIf(u -> u.getId().equals(currentUser.getId()));
                    } else {
                        projectToLike.getLikedByUsers().add(currentUser);
                    }

                    projectToLike.setStarsCount(projectToLike.getLikedByUsers().size());

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
        responseDTO.setAdminComment(project.getAdminComment());
        responseDTO.setReviewedBy(project.getReviewedBy());
        responseDTO.setReviewedAt(project.getReviewedAt());
        responseDTO.setGithubUrl(project.getGithubUrl());
        responseDTO.setCreatedAt(project.getCreatedAt()); // Fixed
        responseDTO.setUpdatedAt(project.getUpdatedAt());
        responseDTO.setTags(project.getTags());

        // Map AI summary (stored as JSON in Project.aiSummary) onto flat DTO fields.
        if (project.getAiSummary() != null && !project.getAiSummary().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(project.getAiSummary());
                JsonNode summaryNode = root.path("summary");
                if (!summaryNode.isMissingNode()) {
                    responseDTO.setAiSummaryProblem(getNullableText(summaryNode, "problem"));
                    responseDTO.setAiSummarySolution(getNullableText(summaryNode, "solution"));
                    responseDTO.setAiSummaryTechnologies(getNullableText(summaryNode, "technologies"));
                    responseDTO.setAiSummaryImpact(getNullableText(summaryNode, "impact"));
                }
            } catch (Exception ignored) {
                // If parsing fails, fall back later based on description/techStack.
            }
        }

        // AI tags are stored as a JSON-mapped List<String> on the entity.
        List<String> aiTags = new ArrayList<>();
        if (project.getAiTags() != null) {
            aiTags.addAll(project.getAiTags());
        }

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

        // Fallbacks if AI summary is missing: use original description / tech stack.
        if (responseDTO.getAiSummaryProblem() == null || responseDTO.getAiSummaryProblem().isBlank()) {
            responseDTO.setAiSummaryProblem(project.getDescription());
        }
        if (responseDTO.getAiSummaryTechnologies() == null || responseDTO.getAiSummaryTechnologies().isBlank()) {
            List<String> techs = responseDTO.getTechStack() != null ? responseDTO.getTechStack() : List.of();
            if (!techs.isEmpty()) {
                responseDTO.setAiSummaryTechnologies(String.join(", ", techs));
            }
        }

        // Handle primary author (leader) — includes email and university for the frontend
        User leader = project.getAuthor();
        if (leader != null) {
            AuthorDTO authorDTO = new AuthorDTO();
            authorDTO.setId(leader.getId());
            authorDTO.setFirstName(leader.getFirstName());
            authorDTO.setLastName(leader.getLastName());
            authorDTO.setEmail(leader.getEmail());
            authorDTO.setUniversity(leader.getUniversity());
            authorDTO.setProfileImageUrl(leader.getAvatar());
            authorDTO.setRole("LEADER");
            responseDTO.setAuthor(authorDTO);
        }

        // Build full authors list (leader + any contributors)
        List<ProjectContributor> contributors = projectContributorRepository.findByProjectId(project.getId());
        List<AuthorDTO> authors = contributors.stream()
                .map(pc -> {
                    User u = pc.getUser();
                    AuthorDTO dto = new AuthorDTO();
                    dto.setId(u.getId());
                    dto.setFirstName(u.getFirstName());
                    dto.setLastName(u.getLastName());
                    dto.setEmail(u.getEmail());
                    dto.setUniversity(u.getUniversity());
                    dto.setProfileImageUrl(u.getAvatar());
                    dto.setRole(pc.getRole());
                    return dto;
                })
                .collect(Collectors.toList());

        // Ensure leader is present in the authors list
        if (leader != null) {
            boolean leaderPresent = authors.stream()
                    .anyMatch(a -> Objects.equals(a.getId(), leader.getId()));
            if (!leaderPresent) {
                AuthorDTO leaderDto = new AuthorDTO();
                leaderDto.setId(leader.getId());
                leaderDto.setFirstName(leader.getFirstName());
                leaderDto.setLastName(leader.getLastName());
                leaderDto.setEmail(leader.getEmail());
                leaderDto.setUniversity(leader.getUniversity());
                leaderDto.setProfileImageUrl(leader.getAvatar());
                leaderDto.setRole("LEADER");
                authors.add(0, leaderDto);
            }
        }

        responseDTO.setAuthors(authors);

        // If we still don't have AI tags, fall back to manual tech stack list.
        if (aiTags.isEmpty()) {
            List<String> techs = responseDTO.getTechStack() != null ? responseDTO.getTechStack() : List.of();
            aiTags.addAll(techs);
        }
        responseDTO.setAiTags(aiTags);

        return responseDTO;
    }


    public ProjectResponseDTO incrementViewCount(Long projectId) {
        // Uses JOIN FETCH to avoid a second query for the author inside the mapper.
        Project project = projectRepository.findByIdWithAuthor(projectId)
                .orElseThrow(() -> new RuntimeException("Project Not Found"));

        project.setViewCount(project.getViewCount() + 1);
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

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Only students can be contributors");
        }

        System.out.println("User to add found: " + user.getEmail());

        // Check if user is already a contributor
        if (projectContributorRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new RuntimeException("User is already a contributor");
        }

        // Check if trying to add project owner as contributor
        if (project.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Project owner cannot be added as contributor");
        }


        // Enforce maximum of 4 authors (leader + up to 3 members)
        long existingCount = projectContributorRepository.countByProjectId(projectId);
        boolean leaderAlreadyInJoin = projectContributorRepository
                .existsByProjectIdAndUserId(projectId, project.getAuthor().getId());
        long effectiveCount = existingCount + (leaderAlreadyInJoin ? 0 : 1);
        if (effectiveCount >= 4) {
            throw new RuntimeException("A project can have at most 4 authors");
        }

        // Normalise role
        String normalizedRole = (role == null || role.isBlank()) ? "MEMBER" : role.trim().toUpperCase();
        if (!normalizedRole.equals("LEADER") && !normalizedRole.equals("MEMBER")) {
            normalizedRole = "MEMBER";
        }

        // Create and save contributor
        ProjectContributor contributor = new ProjectContributor(project, user, normalizedRole);
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

    public List<ProjectResponseDTO> getProjectsByUser(User user) {
        Long userId = user.getId();

        // Projects where the user is the leader
        List<Project> ownedProjects = projectRepository.findByAuthorIdWithAuthor(userId);

        // Projects where the user is listed as a contributor/author
        List<ProjectContributor> contributorLinks = projectContributorRepository.findByUserId(userId);

        Map<Long, ProjectResponseDTO> result = new LinkedHashMap<>();

        // First, add owned projects and mark role as LEADER
        for (Project project : ownedProjects) {
            ProjectResponseDTO dto = mapToProjectResponseDTO(project);
            dto.setCurrentUserRole("LEADER");
            result.put(project.getId(), dto);
        }

        // Then add contributed projects, avoiding duplicates
        for (ProjectContributor link : contributorLinks) {
            Project project = link.getProject();
            if (project == null) continue;

            Long projectId = project.getId();
            if (result.containsKey(projectId)) {
                // Already included as leader; ensure role stays LEADER
                ProjectResponseDTO existing = result.get(projectId);
                if (existing.getCurrentUserRole() == null) {
                    existing.setCurrentUserRole("LEADER");
                }
                continue;
            }

            ProjectResponseDTO dto = mapToProjectResponseDTO(project);
            String role = link.getRole();
            if (role == null || role.isBlank()) {
                role = "MEMBER";
            }
            dto.setCurrentUserRole(role.toUpperCase());
            result.put(projectId, dto);
        }

        return new ArrayList<>(result.values());
    }

    public ProjectResponseDTO approveProject(Long id, String comment, User admin) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        project.setStatus(ProjectStatus.APPROVED);
        project.setAdminComment(comment);
        project.setReviewedBy(admin != null ? admin.getId() : null);
        project.setReviewedAt(java.time.LocalDateTime.now());
        return mapToProjectResponseDTO(projectRepository.save(project));
    }

    public ProjectResponseDTO rejectProject(Long id, String comment, User admin) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        project.setStatus(ProjectStatus.REJECTED);
        project.setAdminComment(comment);
        project.setReviewedBy(admin != null ? admin.getId() : null);
        project.setReviewedAt(java.time.LocalDateTime.now());
        return mapToProjectResponseDTO(projectRepository.save(project));
    }

    public ProjectResponseDTO addAdminComment(Long id, String comment, User admin) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        project.setAdminComment(comment);
        project.setReviewedBy(admin != null ? admin.getId() : project.getReviewedBy());
        project.setReviewedAt(java.time.LocalDateTime.now());
        return mapToProjectResponseDTO(projectRepository.save(project));
    }

    private String getNullableText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return (value != null && !value.isBlank()) ? value : null;
    }

}