package com.ideacrate.backend.user;

import com.ideacrate.backend.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Remove PasswordEncoder from the constructor
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegistrationRequestDTO registrationRequest) {
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new IllegalStateException("User with email " + registrationRequest.getEmail() + " already exists.");
        }
        User newUser = new User();
        newUser.setFirstName(registrationRequest.getFirstName());
        newUser.setLastName(registrationRequest.getLastName());
        newUser.setEmail(registrationRequest.getEmail());
        newUser.setUniversity(registrationRequest.getUniversity());

        newUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        newUser.setRole(Role.STUDENT);
        return userRepository.save(newUser);
    }

    public User loginUser(LoginRequestDTO loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        // CORRECTLY verify the password using the encoder
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }
        
        return user;
    }
    
}