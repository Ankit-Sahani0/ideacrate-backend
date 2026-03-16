package com.ideacrate.backend.DTO;

import lombok.Data;

@Data
public class AuthorDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;          // Required by frontend api.js mapping
    private String university;     // Required by frontend api.js mapping
    private String profileImageUrl;

    // Role of this author on the project (e.g. LEADER or MEMBER)
    private String role;
}
