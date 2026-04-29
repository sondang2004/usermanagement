package com.example.usermanagement.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // Cho phép swagger
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // Cho phép các API mới
                                                .requestMatchers("/api/v1/**").permitAll()

                                                // Còn lại cho phép hết để dễ test (vì là mini project)
                                                .anyRequest().permitAll());

                return http.build();
        }
}
