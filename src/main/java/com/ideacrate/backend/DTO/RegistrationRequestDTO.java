package com.ideacrate.backend.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {
    private String firstName; // Changed from 'name'
    private String lastName;  // Changed from 'name'
    private String email;
    private String password;
    private String university;
}