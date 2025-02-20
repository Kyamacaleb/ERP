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
    public ResponseEntity<List<Map<String, Object>>> getAllLeaves() {
        List<Leave> leaves = leaveService.getAllLeaves();
        List<Map<String, Object>> response = leaves.stream().map(leave -> {
            Map<String, Object> leaveData = new HashMap<>();
            leaveData.put("leaveId", leave.getLeaveId());
            leaveData.put("employeeName", leave.getEmployee().getFullName()); // Get employee name directly
            leaveData.put("leaveType", leave.getLeaveType());
            leaveData.put("startDate", leave.getStartDate());
            leaveData.put("endDate", leave.getEndDate());
            leaveData.put("approverName", leave.getApproverName());
            leaveData.put("dateRequested", leave.getDateRequested());
            leaveData.put("status", leave.getStatus());
            leaveData.put("daysTaken", leave.getDaysTaken());
            return leaveData;
        }).toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
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

    // Get current employee's leave history (for Employee)
    @GetMapping("/me/history")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Leave>> getEmployeeLeaveHistory() {
        List<Leave> leaves = leaveService.getLeavesByCurrentEmployee(); // You may want to implement a separate method for history
        return new ResponseEntity<>(leaves, HttpStatus.OK);
    }
    // Get all leave history (for Admin)
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllLeaveHistory() {
        List<Leave> leaves = leaveService.getAllLeaveHistory();
        List<Map<String, Object>> response = leaves.stream().map(leave -> {
            Map<String, Object> leaveData = new HashMap<>();
            leaveData.put("leaveId", leave.getLeaveId());
            leaveData.put("employeeName", leave.getEmployee().getFullName()); // Get employee name directly
            leaveData.put("leaveType", leave.getLeaveType());
            leaveData.put("startDate", leave.getStartDate());
            leaveData.put("endDate", leave.getEndDate());
            leaveData.put("status", leave.getStatus());
            leaveData.put("dateRequested", leave.getDateRequested());
            leaveData.put("daysTaken", leave.getDaysTaken());
            leaveData.put("approverName", leave.getApproverName());
            return leaveData;
        }).toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    // Recall a leave request (for Admin)
    @PutMapping("/{id}/recall")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> recallLeave(@PathVariable UUID id) {
        leaveService.recallLeave(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    // Filter leave requests
    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Leave>> getAllLeaves(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type) {
        List<Leave> leaves = leaveService.filterLeaves(status, type);
        return ResponseEntity.ok(leaves);
    }

    // View leave statistics
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLeaveStatistics() {
        return new ResponseEntity<>(leaveService.getLeaveStatistics(), HttpStatus.OK);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveLeave(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approverName = auth.getName();
        leaveService.approveLeave(id, approverName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    // Get current employee's pending leave requests
    @GetMapping("/me/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<Leave>> getPendingLeaves() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeEmail = authentication.getName();
        List<Leave> pendingLeaves = leaveService.getPendingLeavesByCurrentEmployee(employeeEmail);
        return new ResponseEntity<>(pendingLeaves, HttpStatus.OK);
    }

}