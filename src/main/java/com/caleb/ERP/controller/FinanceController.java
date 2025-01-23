package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Finance;
import com.caleb.ERP.service.FinanceService;
import com.caleb.ERP.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finances")
public class FinanceController {
    @Autowired
    private FinanceService financeService;

    @Autowired
    private EmployeeService employeeService;

    // Create a new requisition record
    @PostMapping(value = "/requisition", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> createRequisition(
            @RequestParam("purpose") String purpose,
            @RequestParam("amount") double amount,
            @RequestParam("dateSubmitted") String dateSubmitted, // Accept the date from the form
            @RequestParam(value = "file", required = false) MultipartFile file) {

        Finance finance = new Finance();
        finance.setPurpose(purpose);
        finance.setAmount(amount);
        finance.setType("Requisition");
        finance.setDateSubmitted(dateSubmitted); // Set the date from the request
        finance.setStatus("Pending"); // Default status
        return createFinanceRecord(finance, file);
    }

    // Create a new claim record
    @PostMapping(value = "/claim", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> createClaim(
            @RequestParam("expenseType") String expenseType,
            @RequestParam("amount") double amount,
            @RequestParam("dateSubmitted") String dateSubmitted, // Accept the date from the form
            @RequestParam(value = "file", required = false) MultipartFile file) {

        Finance finance = new Finance();
        finance.setExpenseType(expenseType);
        finance.setAmount(amount);
        finance.setType("Claim");
        finance.setDateSubmitted(dateSubmitted); // Set the date from the request
        finance.setStatus("Pending"); // Default status
        return createFinanceRecord(finance, file);
    }

    private ResponseEntity<Finance> createFinanceRecord(Finance finance, MultipartFile file) {
        // Get the logged-in employee's ID
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employeeId = employeeService.getEmployeeByEmail(email)
                .map(employee -> employee.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Set the employee for the finance record
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        finance.setEmployee(employee);

        // If a file is provided, save it to the server and store the path
        if (file != null && !file.isEmpty()) {
            String filePath = saveFileToServer(file);
            finance.setSupportingDocuments(filePath);
        }

        try {
            Finance createdFinance = financeService.createFinance(finance);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFinance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    private String saveFileToServer(MultipartFile file) {
        // Define the directory where files will be saved
        String directoryPath = "/Users/m/Documents/Springboot/ERP/src/main/resources/uploads"; // Change this to your actual path
        File directory = new File(directoryPath);

        // Create the directory if it does not exist
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory and any necessary parent directories
        }

        // Construct the full file path
        String fileName = file.getOriginalFilename();
        String filePath = directoryPath + "/" + fileName; // Full path to save the file
        try {
            File destinationFile = new File(filePath);
            file.transferTo(destinationFile); // Save the file to the specified path
        } catch (IOException e) {
            // Handle file save error
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
        return filePath; // Return the path to the saved file
    }
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
        return financeService.getAllFinances();
    }

    // Get a finance record by ID (for Admin and Employee)
    @GetMapping("/{financeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> getFinanceById(@PathVariable UUID financeId) {
        return financeService.getFinanceById(financeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update an existing finance record (for Employee only)
    @PutMapping("/{financeId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Finance> updateFinance(@PathVariable UUID financeId, @RequestBody Finance financeDetails) {
        // Ensure the employee is updating their own record
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employeeId = employeeService.getEmployeeByEmail(email)
                .map(employee -> employee.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        Finance existingFinance = financeService.getFinanceById(financeId)
                .orElseThrow(() -> new IllegalArgumentException("Finance record not found"));

        if (!existingFinance.getEmployee().getEmployeeId().equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // Employee cannot update another's record
        }

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
        financeService.approveFinance(financeId, feedback);
        return ResponseEntity.ok().build();
    }

    // Reject a finance record (for Admin)
    @PatchMapping("/{financeId}/reject")
    @PreAuthorize("hasRole ('ADMIN')")
    public ResponseEntity<Void> rejectFinance(@PathVariable UUID financeId, @RequestBody String feedback) {
        financeService.rejectFinance(financeId, feedback);
        return ResponseEntity.ok().build();
    }

    // Recall an approved finance record (for Admin)
    @PatchMapping("/{financeId}/recall")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> recallFinance(@PathVariable UUID financeId) {
        financeService.recallFinance(financeId);
        return ResponseEntity.ok().build();
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

    // Get finance history for a specific employee (for Admin)
    @GetMapping("/history/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Finance>> getEmployeeFinanceHistoryByAdmin(@PathVariable UUID employeeId) {
        List<Finance> financeHistory = financeService.getFinanceHistoryByEmployee(employeeId);
        return ResponseEntity.ok(financeHistory);
    }

    @GetMapping("/{financeId}/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Resource> downloadFinanceFile(@PathVariable UUID financeId) {
        Finance finance = financeService.getFinanceById(financeId)
                .orElseThrow(() -> new RuntimeException("Finance record not found"));

        String filePath = finance.getSupportingDocuments();
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}