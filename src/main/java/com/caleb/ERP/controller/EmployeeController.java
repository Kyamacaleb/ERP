package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.service.EmployeeService;
import com.caleb.ERP.service.JwtTokenService;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata; // This is the correct import
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Employee loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Log the current user and their authorities
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user: " + currentAuth.getName());
        System.out.println("Authorities: " + currentAuth.getAuthorities());

        // Retrieve the employee details
        Employee employee = employeeService.getEmployeeByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Get the role and employee ID
        String role = "ROLE_" + employee.getRole(); // Add "ROLE_" prefix to the role
        String employeeId = employee.getEmployeeId().toString();

        // Generate the JWT with the prefixed role
        String jwt = jwtTokenService.generateAccessToken(authentication.getName(), role, employeeId);
        System.out.println("Generated JWT: " + jwt); // Log the JWT

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("jwt", jwt);
        response.put("role", role); // This will now include the "ROLE_" prefix

        return ResponseEntity.ok(response);
    }
    // Admin create employee
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        if (employeeService.existsByEmail(employee.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists. Please use a different email.");
        }

        Employee createdEmployee = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    // Admin update employee
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody Employee employeeDetails) {
        try {
            Employee updatedEmployee = employeeService.updateEmployee(id, employeeDetails);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Admin deactivate employee
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateEmployee(@PathVariable UUID id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }

    // Get all employees (for ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    // Get deactivated employees (for ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deactivated")
    public ResponseEntity<List<Employee>> getDeactivatedEmployees() {
        List<Employee> deactivatedEmployees = employeeService.getDeactivatedEmployees();
        return ResponseEntity.ok(deactivatedEmployees);
    }

    // Get employee by ID ( for both ADMIN and EMPLOYEE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployee(@PathVariable UUID id) {
        Optional<Employee> employee = employeeService.getEmployeeById(id);
        return employee.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get current employee details (for EMPLOYEE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me")
    public ResponseEntity<Employee> getCurrentEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Employee> employee = employeeService.getEmployeeByEmail(email);
        return employee.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Employee update their own details
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/me")
    public ResponseEntity<Employee> updateCurrentEmployee(@RequestBody Employee employeeDetails) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeService.getEmployeeByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Update the employee's details (excluding email)
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setEmergencyContactName(employeeDetails.getEmergencyContactName());
        employee.setEmergencyContactPhone(employeeDetails.getEmergencyContactPhone());
        employee.setDepartment(employeeDetails.getDepartment());

        Employee updatedEmployee = employeeService.updateEmployee(employee.getEmployeeId(), employee);
        return ResponseEntity.ok(updatedEmployee);
    }

    // Endpoint for uploading profile picture
    @PostMapping("/{id}/upload-profile-picture")
    public ResponseEntity<String> uploadProfilePicture(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        // Validate the file (size, type, etc.)
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        // Check if the image is upright
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                if (orientation != 1) { // 1 means upright
                    return ResponseEntity.badRequest().body("Image must be upright.");
                }
            }
        } catch (IOException | ImageProcessingException | MetadataException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading image metadata.");
        }

        // Logic to save the file
        String uploadsDir = "src/main/resources/uploads/";
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename(); // Unique file name
        File destinationFile = new File(uploadsDir + fileName);

        try {
            file.transferTo(destinationFile); // Save the file
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving file.");
        }

        // Update the employee's profile picture
        Employee employee = employeeService.getEmployeeById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        employee.setProfilePicture(fileName); // Save the file name
        employeeService.updateEmployee(id, employee); // Update employee record

        return ResponseEntity.ok("Profile picture uploaded successfully.");
    }

    // Change password for the current employee
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/me/change-password")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String newPassword = request.get("newPassword");
        String currentPassword = request.get("currentPassword");

        Employee employee = employeeService.getEmployeeByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Validate the current password (you may need to implement this logic)
        if (!passwordEncoder.matches(currentPassword, employee.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect.");
        }

        employee.setPassword(passwordEncoder.encode(newPassword)); // Hash the new password
        employeeService.updateEmployee(employee.getEmployeeId(), employee); // Update the employee record

        return ResponseEntity.ok("Password changed successfully.");
    }

    // Endpoint for getting the profile picture
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable UUID id) {
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        String fileName = employee.getProfilePicture();
        String uploadsDir = "src/main/resources/uploads/";

        // If no profile picture is set, use the default image
        if (fileName == null || fileName.isEmpty()) {
            fileName = "default-profile.png"; // Default image
        }

        File file = new File(uploadsDir + fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build(); // Return 404 if the file does not exist
        }

        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG) // Adjust based on your image type
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}