package com.ideacrate.backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data // Generates getters, setters, toString(), etc.
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class LoginResponseDTO {
    private String token;
    private UserResponseDTO user;
}