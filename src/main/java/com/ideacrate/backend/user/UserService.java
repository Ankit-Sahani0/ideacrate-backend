package com.ideacrate.backend.user;

import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Remove PasswordEncoder from the constructor
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        // WARNING: Storing plain text password (temporary for learning)
        newUser.setPassword(registrationRequest.getPassword());

        newUser.setRole("USER");
        return userRepository.save(newUser);
    }

    public User loginUser(LoginRequestDTO loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        // WARNING: Comparing plain text passwords (temporary for learning)
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }
        return user;
    }
}