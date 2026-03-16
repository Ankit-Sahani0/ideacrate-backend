package com.ideacrate.backend.project;

import lombok.Data;

@Data
public class AuthorDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String profileImageUrl;

}
