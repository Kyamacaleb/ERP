package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Finance;
import com.caleb.ERP.service.FinanceService;
import com.caleb.ERP.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finances")
public class FinanceController {
    @Autowired
    private FinanceService financeService;

    @Autowired
    private EmployeeService employeeService;

    // Get all finances for the logged-in employee
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Finance>> getAllFinancesForEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employeeId = employeeService.getEmployeeByEmail(email)
                .map(employee -> employee.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        List<Finance> finances = financeService.getAllFinancesByEmployee(employeeId);
        return ResponseEntity.ok(finances);
    }

    // Get all finances (for Admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Finance> getAllFinances() {
        return financeService.getAllFinances(); // Admin can view all finance records
    }

    // Create a new finance record (for Employee)
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> createFinance(
            @RequestParam("finance") String financeJson,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // Convert financeJson to Finance object
        ObjectMapper objectMapper = new ObjectMapper();
        Finance finance;
        try {
            finance = objectMapper.readValue(financeJson, Finance.class);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build(); // Handle JSON parsing error
        }

        // Get the logged-in employee's ID
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employeeId = employeeService.getEmployeeByEmail(email)
                .map(employee -> employee.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Set the employee for the finance record
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        finance.setEmployee(employee); // Associate the finance record with the employee

        // If a file is provided, save it to the server and store the path
        if (file != null && !file.isEmpty()) {
            String filePath = saveFileToServer(file); // Call the method to save the file
            finance.setSupportingDocuments(filePath); // Save the file path in the Finance object
        }

        Finance createdFinance = financeService.createFinance(finance);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFinance);
    }

    // Get a finance record by ID (for Admin and Employee)
    @GetMapping("/{financeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> getFinanceById(@PathVariable UUID financeId) {
        return financeService.getFinanceById(financeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update an existing finance record (for Admin)
    @PutMapping("/{financeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Finance> updateFinance(@PathVariable UUID financeId, @RequestBody Finance financeDetails) {
        Finance updatedFinance = financeService.updateFinance(financeId, financeDetails);
        return ResponseEntity.ok(updatedFinance);
    }

    // Delete a finance record (for Admin)
    @DeleteMapping("/{financeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFinance(@PathVariable UUID financeId) {
        financeService.deleteFinance(financeId);
        return ResponseEntity.noContent().build();
    }

    // Approve a finance record (for Admin)
    @PatchMapping("/{financeId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveFinance(@PathVariable UUID financeId, @RequestBody String feedback) {
        financeService.approveFinance(financeId, feedback); // Pass feedback to the service
        return ResponseEntity.ok().build();
    }

    // Reject a finance record (for Admin)
    @PatchMapping("/{financeId}/reject")
    @PreAuthorize("hasRole ('ADMIN')")
    public ResponseEntity<Void> rejectFinance(@PathVariable UUID financeId, @RequestBody String feedback) {
        financeService.rejectFinance(financeId, feedback); // Pass feedback to the service
        return ResponseEntity.ok().build();
    }

    private String saveFileToServer(MultipartFile file) {
        // Use an absolute path to the uploads directory
        String directoryPath = "/Users/m/Documents/Springboot/ERP/src/main/resources/uploads"; // Change this to your actual path
        File directory = new File(directoryPath);

        // Create the directory if it does not exist
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory and any necessary parent directories
        }

        String fileName = file.getOriginalFilename();
        String filePath = directoryPath + "/" + fileName; // Construct the full file path
        try {
            File destinationFile = new File(filePath);
            file.transferTo(destinationFile); // Save the file to the specified path
        } catch (IOException e) {
            // Handle file save error
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
        return filePath; // Return the path to the saved file
    }
    // Get finance history for the logged-in employee
    @GetMapping("/me/history")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Finance>> getEmployeeFinanceHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employeeId = employeeService.getEmployeeByEmail(email)
                .map(employee -> employee.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        List<Finance> financeHistory = financeService.getFinanceHistoryByEmployee(employeeId);
        return ResponseEntity.ok(financeHistory);
    }
}