package com.caleb.ERP.config;

import com.caleb.ERP.service.JwtTokenService;
import com.caleb.ERP.service.EmployeeService; // Import your EmployeeService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService);

        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers("/api/employees/login", "/error").permitAll() // Allow access to login endpoint
                .requestMatchers("/uploads/**").permitAll() // Allow public access to uploaded files
                .requestMatchers("/websocket/**").permitAll() // Allow access to WebSocket endpoint
                .requestMatchers("/favicon.ico", "/images/**", "/error", "/uploads/**", "/css/**", "/js/**", "/").permitAll() // Public resources
                .requestMatchers("/api/employees/me").authenticated() // Ensure this endpoint is authenticated
                .requestMatchers("/api/employees/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/contacts/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/leaves/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/finances/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/notifications/**").permitAll() // Allow access to WebSocket notifications
                .requestMatchers("/employee-dashboard", "/admin-dashboard").permitAll() // Public pages
                .requestMatchers("/admin-dashboard").hasRole("ADMIN")
                // Specific access to profile pictures by employee or admin
                .requestMatchers("/api/employees/{id}/profile-picture").hasAnyRole("EMPLOYEE", "ADMIN")
                .anyRequest().authenticated() // Catch-all for any other requests
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // Return the configured SecurityFilterChain
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, EmployeeService employeeService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(employeeService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Define BCryptPasswordEncoder for password hashing
    }
}
