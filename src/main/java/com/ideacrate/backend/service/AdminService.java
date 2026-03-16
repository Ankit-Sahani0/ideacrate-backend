package com.ideacrate.backend.service;

import com.ideacrate.backend.DTO.ProjectResponseDTO;
import com.ideacrate.backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final ProjectService projectService;

    public AdminService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public List<ProjectResponseDTO> getAllProjects() {
        return projectService.getAllProjectsForAdmin();
    }

    public List<ProjectResponseDTO> getPendingProjects() {
        return projectService.getPendingProjects();
    }

    public List<ProjectResponseDTO> getReviewedProjects() {
        return projectService.getReviewedProjects();
    }

    public ProjectResponseDTO approveProject(Long id, String comment, User admin) {
        return projectService.approveProject(id, comment, admin);
    }

    public ProjectResponseDTO rejectProject(Long id, String comment, User admin) {
        return projectService.rejectProject(id, comment, admin);
    }

    public ProjectResponseDTO addComment(Long id, String comment, User admin) {
        return projectService.addAdminComment(id, comment, admin);
    }
}
