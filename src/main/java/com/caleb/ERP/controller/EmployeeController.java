package com.caleb.ERP.controller;

import com.caleb.ERP.dto.DepartmentStatistics;
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
import java.util.*;

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
        // Validate email format
        if (!employeeService.isValidEmail(loginRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Incorrect email format."));
        }

        // Check if the employee exists
        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmail(loginRequest.getEmail());
        if (employeeOpt.isEmpty()) {
            // If the email is not found, return an error indicating the email is incorrect
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Incorrect email."));
        }

        Employee employee = employeeOpt.get();

        // Check if the password meets complexity requirements
        if (!employeeService.isValidPassword(loginRequest.getPassword())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Password must be at least 8 characters long and include uppercase letter, lowercase letter, special character, and number."));
        }

        // Check if the password is correct
        if (!passwordEncoder.matches(loginRequest.getPassword(), employee.getPassword())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Incorrect password."));
        }

        // Set the authentication in the security context
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Log the current user and their authorities
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user: " + currentAuth.getName());
        System.out.println("Authorities: " + currentAuth.getAuthorities());

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
        try {
            Employee createdEmployee = employeeService.createEmployee(employee);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        System.out.println("Received file: " + file.getOriginalFilename() + ", size: " + file.getSize());

        int orientation = 1; // Default upright
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            System.out.println("Error reading image metadata: " + e.getMessage());
            // Assume upright if metadata cannot be read
        }

        if (orientation != 1) {
            return ResponseEntity.badRequest().body("Image must be upright.");
        }

        // Use user home directory to create uploads directory (cross-platform)
        String uploadsDir = System.getProperty("user.home") + "/uploads/";
        File uploadDir = new File(uploadsDir);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs(); // Create directory if it doesn't exist
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename(); // Unique file name
        File destinationFile = new File(uploadsDir + fileName);

        try {
            // Debugging statement to check file path before saving
            System.out.println("Saving file to: " + destinationFile.getAbsolutePath());

            file.transferTo(destinationFile); // Save the file
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving file.");
        }

        // Update the employee's profile picture
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
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

        try {
            // Assuming you have a method to get the employee ID by email
            UUID employeeId = employeeService.getEmployeeByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"))
                    .getEmployeeId();

            employeeService.changePassword(employeeId, currentPassword, newPassword);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint for getting the profile picture
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable UUID id) {
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        String fileName = employee.getProfilePicture();
        // Use user home directory for storing uploaded images (cross-platform)
        String uploadsDir = System.getProperty("user.home") + "/uploads/";

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
                    .contentType(MediaType.IMAGE_PNG) // Adjust based on your image type (e.g., PNG, JPEG)
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/reset-password")
    @PreAuthorize("hasRole('ADMIN')") // Restrict access to users with the ADMIN role
    public ResponseEntity<String> resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
        try {
            employeeService.resetEmployeePasswordByEmail(email, newPassword);
            return ResponseEntity.ok("Password reset successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/department-stats")
    public ResponseEntity<List<DepartmentStatistics>> getEmployeeCountByDepartment() {
        List<DepartmentStatistics> stats = employeeService.getEmployeeCountByDepartment();
        return ResponseEntity.ok(stats);
    }

}