package com.ideacrate.backend.security;

import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.enums.Role;
import com.ideacrate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    // ------------------------------------------------------------------
    // UserDetailsService — tells Spring how to load a user by email.
    // Used by JwtAuthFilter on every protected request.
    // ------------------------------------------------------------------
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    // ------------------------------------------------------------------
    // AuthenticationProvider — wires UserDetailsService + PasswordEncoder.
    // ------------------------------------------------------------------
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // ------------------------------------------------------------------
    // AuthenticationManager — used internally by Spring Security.
    // ------------------------------------------------------------------
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ------------------------------------------------------------------
    // PasswordEncoder — BCrypt is the correct encoder used at registration.
    // ------------------------------------------------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@ideacrate.local";

            if (userRepository.findByEmail(adminEmail).isPresent()) {
                return; // Admin already exists
            }

            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail(adminEmail);
            admin.setUniversity("System");
            admin.setPassword(passwordEncoder.encode("AdminPass123!"));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
        };
    }

    // ------------------------------------------------------------------
    // CorsConfigurationSource — THIS IS THE CRITICAL FIX.
    //
    // When SecurityConfiguration uses .cors(Customizer.withDefaults()),
    // Spring Security looks for a CorsConfigurationSource bean in the
    // application context. Without this bean, it falls back to a default
    // that BLOCKS all cross-origin requests with 403 Forbidden.
    //
    // We previously only had a WebMvcConfigurer (in WebConfig.java), which
    // handles CORS at the Spring MVC layer. But Spring Security's CORS
    // filter runs BEFORE Spring MVC, so the MVC config was never reached.
    // ------------------------------------------------------------------
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed frontend origins — add production domain here when deploying
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // Create React App (legacy)
                "http://localhost:5173"    // Vite dev server (current)
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers the frontend might send (Content-Type, Authorization, etc.)
        config.setAllowedHeaders(List.of("*"));

        // Allow the frontend to read the Authorization header from responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies/credentials to be sent (needed for some auth flows)
        config.setAllowCredentials(true);

        // Cache the preflight (OPTIONS) response for 1 hour to reduce overhead
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}