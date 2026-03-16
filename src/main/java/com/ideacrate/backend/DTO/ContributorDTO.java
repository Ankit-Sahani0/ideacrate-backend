package com.ideacrate.backend.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContributorDTO {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImageUrl;
    private String role;
    private LocalDateTime addedAt;
}
