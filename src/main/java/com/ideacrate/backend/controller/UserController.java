package com.ideacrate.backend.controller;

import com.ideacrate.backend.DTO.UserResponseDTO;
import com.ideacrate.backend.DTO.UserSearchDTO;
import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(
            userService.getAllUsers().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList())
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UserResponseDTO updateRequest) {
        
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setUniversity(updateRequest.getUniversity());
        
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDTO>> searchStudents(@RequestParam("query") String query) {
        List<UserSearchDTO> results = userService.searchStudents(query).stream()
                .map(user -> {
                    UserSearchDTO dto = new UserSearchDTO();
                    dto.setId(user.getId());
                    dto.setName(user.getFirstName() + " " + user.getLastName());
                    dto.setEmail(user.getEmail());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setUniversity(user.getUniversity());
        dto.setRole(user.getRole());
        return dto;
    }
}
