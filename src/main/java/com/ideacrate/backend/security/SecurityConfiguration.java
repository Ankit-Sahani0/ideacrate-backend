package com.ideacrate.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // FIX 1: Use Customizer.withDefaults() so Spring Security picks up the
                // CorsConfigurationSource bean registered in ApplicationConfig.
                // The previous .cors(cors -> {}) used an EMPTY lambda which bypassed
                // the WebMvcConfigurer CORS setup and blocked all cross-origin requests.
                .cors(Customizer.withDefaults())

                // CSRF is disabled — this is correct for a stateless JWT API.
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // --- Public endpoints (no token needed) ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()             // Preflight
                        .requestMatchers("/api/v1/auth/**").permitAll()                     // Login & register
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll()    // Browse projects
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/**").permitAll() // Project details & search
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/view").permitAll() // View count

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/projects/pending").hasRole("ADMIN")
                        .requestMatchers("/api/v1/projects/*/approve").hasRole("ADMIN")
                        .requestMatchers("/api/v1/projects/*/reject").hasRole("ADMIN")

                        // Swagger UI — public
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // --- Authenticated endpoints ---
                        .requestMatchers("/api/v1/users/**").authenticated()                // User profile
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/projects/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/like").authenticated()
                        .requestMatchers("/api/v1/projects/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/contributors").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/*/contributors/*").authenticated()

                        // Catch-all — require authentication for anything not explicitly listed
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}