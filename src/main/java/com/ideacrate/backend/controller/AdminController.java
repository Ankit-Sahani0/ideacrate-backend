package com.ideacrate.backend.controller;

import com.ideacrate.backend.DTO.ProjectResponseDTO;
import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/projects")
	public List<ProjectResponseDTO> getAllProjects() {
		return adminService.getAllProjects();
	}

	// Convenience alias: returns every project (any status) for admins.
	@GetMapping("/projects/all")
	public List<ProjectResponseDTO> getAllProjectsAnyStatus() {
		return adminService.getAllProjects();
	}

	@GetMapping("/projects/pending")
	public List<ProjectResponseDTO> getPendingProjects() {
		return adminService.getPendingProjects();
	}

	@GetMapping("/projects/reviewed")
	public List<ProjectResponseDTO> getReviewedProjects() {
		return adminService.getReviewedProjects();
	}

	@PostMapping("/projects/{id}/approve")
	public ResponseEntity<ProjectResponseDTO> approveProject(@PathVariable Long id,
															 @RequestBody(required = false) AdminActionRequest request,
															 @AuthenticationPrincipal User admin) {
		ProjectResponseDTO dto = adminService.approveProject(id, request != null ? request.getComment() : null, admin);
		return ResponseEntity.ok(dto);
	}

	@PostMapping("/projects/{id}/reject")
	public ResponseEntity<ProjectResponseDTO> rejectProject(@PathVariable Long id,
															@RequestBody(required = false) AdminActionRequest request,
															@AuthenticationPrincipal User admin) {
		ProjectResponseDTO dto = adminService.rejectProject(id, request != null ? request.getComment() : null, admin);
		return ResponseEntity.ok(dto);
	}

	@PostMapping("/projects/{id}/comment")
	public ResponseEntity<ProjectResponseDTO> commentProject(@PathVariable Long id,
															 @RequestBody AdminActionRequest request,
															 @AuthenticationPrincipal User admin) {
		ProjectResponseDTO dto = adminService.addComment(id, request.getComment(), admin);
		return ResponseEntity.ok(dto);
	}

	public static class AdminActionRequest {
		private String comment;

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}
}
