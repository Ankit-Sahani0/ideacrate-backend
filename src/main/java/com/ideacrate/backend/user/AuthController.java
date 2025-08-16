package com.ideacrate.backend.user;

import com.ideacrate.backend.security.JwtService;
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

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody RegistrationRequestDTO registrationRequest) {
        User newUser = userService.registerUser(registrationRequest);

        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(newUser.getId());
        // Use new firstName and lastName fields
        responseDTO.setFirstName(newUser.getFirstName());
        responseDTO.setLastName(newUser.getLastName());
        responseDTO.setEmail(newUser.getEmail());
        responseDTO.setUniversity(newUser.getUniversity());
        responseDTO.setRole(newUser.getRole());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // In AuthController.java

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        // 1. Authenticate the user. This returns the full User entity.
        User authenticatedUser = userService.loginUser(loginRequest);

        // 2. Generate a JWT for that user.
        String token = jwtService.generateToken(authenticatedUser);

        // 3. Create the UserResponseDTO to hold the public user details.
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setId(authenticatedUser.getId());
        userDTO.setFirstName(authenticatedUser.getFirstName());
        userDTO.setLastName(authenticatedUser.getLastName());
        userDTO.setEmail(authenticatedUser.getEmail());
        userDTO.setUniversity(authenticatedUser.getUniversity());
        userDTO.setRole(authenticatedUser.getRole());

        // 4. Build the final response containing both the token and user DTO.
        LoginResponseDTO authResponse = LoginResponseDTO.builder()
                .token(token)
                .user(userDTO)
                .build();

        // 5. Return the response.
        return ResponseEntity.ok(authResponse);
    }
}