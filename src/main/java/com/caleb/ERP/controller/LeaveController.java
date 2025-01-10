package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Leave;
import com.caleb.ERP.service.EmployeeService;
import com.caleb.ERP.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private EmployeeService employeeService;

    // Get all leave requests (for Admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Leave> getAllLeaves() {
        return leaveService.getAllLeaves();
    }

    // Submit a new leave request (for Employee)
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Leave> submitLeaveRequest(@RequestBody Leave leaveRequest) {
        Leave createdLeave = leaveService.submitLeaveRequest(leaveRequest);
        return new ResponseEntity<>(createdLeave, HttpStatus.CREATED);
    }

    // Get leave request by ID (for Admin or Employee)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Leave> getLeaveById(@PathVariable UUID id) {
        Leave leave = leaveService.getLeaveById(id);
        return new ResponseEntity<>(leave, HttpStatus.OK);
    }

    // Approve a leave request (for Admin)
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveLeave(@PathVariable UUID id) {
        leaveService.approveLeave(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Reject a leave request (for Admin)
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectLeave(@PathVariable UUID id) {
        leaveService.rejectLeave(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Get current employee's leave requests (for Employee)
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Leave>> getCurrentEmployeeLeaves() {
        List<Leave> leaves = leaveService.getLeavesByCurrentEmployee();
        return new ResponseEntity<>(leaves, HttpStatus.OK);
    }

    // Update a leave request (for Employee)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Leave> updateLeaveRequest(@PathVariable UUID id, @RequestBody Leave leaveRequest) {
        Leave updatedLeave = leaveService.updateLeaveRequest(id, leaveRequest);
        return new ResponseEntity<>(updatedLeave, HttpStatus.OK);
    }
    @GetMapping("/me/balance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Integer>> getCurrentEmployeeLeaveBalance() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeEmail = authentication.getName();
        Employee employee = employeeService.getEmployeeByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Integer> leaveBalance = new HashMap<>();
        leaveBalance.put("Sick Leave Balance", employee.getSickLeaveBalance());
        leaveBalance.put("Vacation Leave Balance", employee.getVacationLeaveBalance());
        leaveBalance.put("Paternity Leave Balance", employee.getPaternityLeaveBalance());
        leaveBalance.put("Compassionate Leave Balance", employee.getCompassionateLeaveBalance());

        return new ResponseEntity<>(leaveBalance, HttpStatus.OK);
    }

    // Get all leave requests for the current employee
    @GetMapping("/me/history")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Leave>> getEmployeeLeaveHistory() {
        List<Leave> leaves = leaveService.getLeavesByCurrentEmployee();
        return new ResponseEntity<>(leaves, HttpStatus.OK);
    }
}