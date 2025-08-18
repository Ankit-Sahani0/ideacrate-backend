package com.ideacrate.backend.project;

import com.ideacrate.backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public final ProjectRepository projectRepository;

    public ProjectController(ProjectService projectService, ProjectRepository projectRepository) {
        this.projectService = projectService;

        this.projectRepository = projectRepository;
    }

    @GetMapping()
    public List<ProjectResponseDTO> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id)
                .map(projectDTO -> new ResponseEntity<>(projectDTO, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(
            @RequestBody ProjectRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        // Spring Security automatically provides the currently logged-in User object.
        // No need to check for null, as this endpoint will be protected.

        ProjectResponseDTO createdProject = projectService.createProject(request, currentUser);

        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Optional<ProjectResponseDTO> updateProject(@PathVariable Long id,
            @RequestBody ProjectRequestDTO projectRequestDTO) {
        return projectService.updateProject(id, projectRequestDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectById(@PathVariable Long id) {
        boolean wasDeleted = projectService.deleteProject(id);

        if (wasDeleted) {
            // If the service returns true, send back 204 No Content
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            // If the service returns false, send back 404 Not Found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ProjectResponseDTO> likeProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        //System.out.println("=== Like Endpoint Debug ===");
       // System.out.println("Endpoint: /api/v1/projects/" + id + "/like");
       // System.out.println("Method: POST");
        //System.out.println("Current User: " + (currentUser != null ? currentUser.getEmail() : "null"));

        return projectService.likeProject(id, currentUser)
                .map(likedProjectDTO -> new ResponseEntity<>(likedProjectDTO, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**

    @GetMapping("/{projectId}/contributors/{userId}/check")
    public ResponseEntity<Boolean> isContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId) {

        boolean isContributor = projectService.isContributor(projectId, userId);
        return ResponseEntity.ok(isContributor);
    }

    @PostMapping("/{projectId}/contributors/{userId}")
    public ResponseEntity<String> addContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        projectService.addContributor(projectId, userId, currentUser);
        return ResponseEntity.ok("Contributor added successfully");
    }



    @DeleteMapping("/{projectId}/contributors/{userId}")
    public ResponseEntity<String> removeContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        projectService.removeContributor(projectId, userId, currentUser);
        return ResponseEntity.ok("Contributor removed successfully");
    }

    @GetMapping("/{projectId}/contributors")
    public ResponseEntity<List<AuthorDTO>> getProjectContributors(
            @PathVariable Long projectId) {

        List<AuthorDTO> contributors = projectService.getProjectContributors(projectId);
        return ResponseEntity.ok(contributors);
    }
     */

    @PostMapping("/{id}/view")
    public ResponseEntity<ProjectResponseDTO> incrementViewCount(@PathVariable Long id) {
        try {
            ProjectResponseDTO updatedProject = projectService.incrementViewCount(id);
            return ResponseEntity.ok(updatedProject);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{projectId}/contributors")
    public ResponseEntity<List<ContributorDTO>> getProjectContributors(@PathVariable Long projectId) {
        List<ContributorDTO> contributors = projectService.getProjectContributors(projectId);
        return ResponseEntity.ok(contributors);
    }

    @PostMapping("/{projectId}/contributors")
    public ResponseEntity<ContributorDTO> addContributor(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "CONTRIBUTOR") String role,
            @AuthenticationPrincipal User currentUser) {

        System.out.println("=== Add Contributor Debug ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("User ID: " + userId);
        System.out.println("Role: " + role);
        System.out.println("Current User: " + (currentUser != null ? currentUser.getEmail() : "null"));

        if (currentUser == null) {
            System.out.println("Current user is null - authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ContributorDTO contributor = projectService.addContributor(projectId, userId, role, currentUser);
            return ResponseEntity.ok(contributor);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

    }

    @DeleteMapping("/{projectId}/contributors/{userId}")
    public ResponseEntity<Void> removeContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        projectService.removeContributor(projectId, userId, currentUser);
        return ResponseEntity.noContent().build();
    }


    }
