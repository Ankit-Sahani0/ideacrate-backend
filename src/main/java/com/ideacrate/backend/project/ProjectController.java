package com.ideacrate.backend.project;

import com.ideacrate.backend.user.User;
import com.ideacrate.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin("http://localhost:3000")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {


    private final ProjectService projectService;
    private final UserRepository userRepository;
    public final ProjectRepository projectRepository;

    public ProjectController(ProjectService projectService, UserRepository userRepository, ProjectRepository projectRepository){
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping()
    public List<ProjectResponseDTO> getAllProjects(){
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long id ){
        return projectService.getProjectById(id)
                .map(projectDTO -> new ResponseEntity<>(projectDTO, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO request) {
        // get authenticated user's email/username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            }
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User author = optUser.get();
        ProjectResponseDTO created = projectService.createProject(request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Optional<ProjectResponseDTO> updateProject(@PathVariable Long id,@RequestBody ProjectRequestDTO projectRequestDTO){
            return projectService.updateProject(id,projectRequestDTO);
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
    public ResponseEntity<ProjectResponseDTO> likeProject(@PathVariable Long id) {
        return projectService.likeProject(id)
                .map(likedProjectDTO -> new ResponseEntity<>(likedProjectDTO, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    }


