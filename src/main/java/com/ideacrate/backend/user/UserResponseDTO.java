package com.ideacrate.backend.user;

import com.ideacrate.backend.enums.Role;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String firstName; // Changed from 'name'
    private String lastName;  // Changed from 'name'
    private String email;
    private String university;
    private Role role;
}