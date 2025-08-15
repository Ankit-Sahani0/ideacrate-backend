package com.ideacrate.backend.user;

import com.ideacrate.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("http://localhost:3000")
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        // This part stays the same: it validates the user's password.
        // If it's wrong, it will throw an exception.
        User authenticatedUser = userService.loginUser(loginRequest);

        // If the login was successful, generate a token for that user.
        String token = jwtService.generateToken(authenticatedUser);

        // Create the response object containing the token.
        LoginResponseDTO responseDTO = new LoginResponseDTO(token);

        // Return the token in the response.
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}