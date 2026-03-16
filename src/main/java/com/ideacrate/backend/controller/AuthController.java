package com.ideacrate.backend.controller;

import com.ideacrate.backend.DTO.LoginRequestDTO;
import com.ideacrate.backend.DTO.LoginResponseDTO;
import com.ideacrate.backend.DTO.RegistrationRequestDTO;
import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.security.JwtService;
import com.ideacrate.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/v1/auth/register
     *
     * Registers a new user and immediately returns a JWT token so the
     * frontend can log the user in right after signup — no second login needed.
     *
     * BUG FIXED: Previously returned UserResponseDTO (no token field).
     * The frontend called localStorage.setItem("token", data.token) which
     * stored undefined, so the user was never authenticated after signup.
     *
     * BUG FIXED: role was commented out — restored so frontend AuthContext
     * can derive isAdmin correctly.
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> registerUser(@RequestBody RegistrationRequestDTO registrationRequest) {
        User newUser = userService.registerUser(registrationRequest);

        // Generate JWT immediately so the user is logged in after registration
        String token = jwtService.generateToken(newUser);

        LoginResponseDTO response = buildLoginResponse(token, newUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * POST /api/v1/auth/login
     *
     * Authenticates a user and returns a JWT token + user details.
     *
     * BUG FIXED: role was commented out — the frontend AuthContext computes
     * isAdmin as (user.role === "admin"). With role null, isAdmin was always
     * false and the Admin page was inaccessible even for admin users.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        User authenticatedUser = userService.loginUser(loginRequest);
        String token = jwtService.generateToken(authenticatedUser);

        LoginResponseDTO response = buildLoginResponse(token, authenticatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Builds the login/register response from a User entity.
     *
     * The LoginResponseDTO shape is: { token, id, firstName, lastName, email, role, university }
     * This flat structure matches exactly what api.js expects on data.id, data.role, etc.
     */
    private LoginResponseDTO buildLoginResponse(String token, User user) {
        return LoginResponseDTO.builder()
                .token(token)
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())        // e.g. "STUDENT", "ADMIN"
                .university(user.getUniversity())
                .build();
    }
}