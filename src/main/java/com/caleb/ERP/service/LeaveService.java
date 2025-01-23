package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Leave;
import com.caleb.ERP.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LeaveService {
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private EmployeeService employeeService;

    public Leave submitLeaveRequest(Leave leaveRequest) {
        // Validate leave request

        // 1. Check if the leave type is valid
        List<String> validLeaveTypes = List.of("Sick", "Vacation", "Paternity/Maternity", "Compassionate");
        if (!validLeaveTypes.contains(leaveRequest.getLeaveType())) {
            throw new IllegalArgumentException("Invalid leave type. Allowed types are: " + validLeaveTypes);
        }

        // 2. Check if the start date is not in the past
        LocalDate today = LocalDate.now();
        LocalDate startDate = LocalDate.parse(leaveRequest.getStartDate());
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }

        // 3. Check if the end date is after the start date
        LocalDate endDate = LocalDate.parse(leaveRequest.getEndDate());
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after the start date.");
        }

        // 4. Calculate the number of leave days
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Include the end date
        leaveRequest.setDaysTaken((int) daysBetween); // Set the number of days taken
        leaveRequest.setDaysAllocated(21); // Set the allocated days (constant value)

        // Set the date requested to the current date
        leaveRequest.setDateRequested(LocalDate.now().toString()); // Set to current date

        // 5. Set initial status
        leaveRequest.setStatus("Pending");

        // 6. Retrieve the authenticated employee's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeEmail = authentication.getName(); // Assuming the email is used as the username
        Employee employee = employeeService.getEmployeeByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Set the employee in the leave request
        leaveRequest.setEmployee(employee); // Set the entire Employee object

        // 7. Save the leave request
        return leaveRepository.save(leaveRequest);
    }

    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    public Leave getLeaveById(UUID id) {
        return leaveRepository.findById(id).orElseThrow(() -> new RuntimeException("Leave not found"));
    }

    public void approveLeave(UUID id, String approverName) {
        Leave leave = getLeaveById(id);

        // Validate the leave status
        if ("Approved".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been approved.");
        }
        if ("Rejected".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been rejected.");
        }

        Employee employee = leave.getEmployee();

        // Check leave balance
        switch (leave.getLeaveType()) {
            case "Sick":
                if (leave.getDaysTaken() > employee.getSickLeaveBalance()) {
                    throw new IllegalArgumentException("Insufficient sick leave balance.");
                }
                employee.setSickLeaveBalance(employee.getSickLeaveBalance() - leave.getDaysTaken());
                break;
            case "Vacation":
                if (leave.getDaysTaken() > employee.getVacationLeaveBalance()) {
                    throw new IllegalArgumentException("Insufficient vacation leave balance.");
                }
                employee.setVacationLeaveBalance(employee.getVacationLeaveBalance() - leave.getDaysTaken());
                break;
            case "Paternity/Maternity":
                if (leave.getDaysTaken() > employee.getPaternityLeaveBalance()) {
                    throw new IllegalArgumentException("Insufficient paternity leave balance.");
                }
                employee.setPaternityLeaveBalance(employee.getPaternityLeaveBalance() - leave.getDaysTaken());
                break;
            case "Compassionate":
                if (leave.getDaysTaken() > employee.getCompassionateLeaveBalance()) {
                    throw new IllegalArgumentException("Insufficient compassionate leave balance.");
                }
                employee.setCompassionateLeaveBalance(employee.getCompassionateLeaveBalance() - leave.getDaysTaken());
                break;
            default:
                throw new IllegalArgumentException("Invalid leave type.");
        }

        leave.setStatus("Approved");
        leave.setApproverName(approverName);
        leaveRepository.save(leave);

        // Update employee balances in the database
        employeeService.updateEmployee(employee.getEmployeeId(), employee);
    }

    public void rejectLeave(UUID id) {
        Leave leave = getLeaveById(id);

        // Check if the leave request is already approved or rejected
        if ("Approved".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been approved.");
        }
        if ("Rejected".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been rejected.");
        }
        leave.setStatus("Rejected");
        leaveRepository.save(leave);
    }

    public List<Leave> getLeavesByCurrentEmployee() {
        // Retrieve the currently authenticated employee's email or ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeEmail = authentication.getName(); // Assuming the email is used as the username

        // Find the employee by email (you may need to adjust this based on your Employee entity)
        Employee employee = employeeService.getEmployeeByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Retrieve and return the leaves associated with the employee
        return leaveRepository.findByEmployee(employee);
    }

    // Filter leave requests
    public List<Leave> filterLeaves(String status, String type) {
        if (status != null && type != null) {
            return leaveRepository.findByStatusAndLeaveType(status, type);
        } else if (status != null) {
            return leaveRepository.findByStatus(status);
        } else if (type != null) {
            return leaveRepository.findByLeaveType(type);
        }
        return leaveRepository.findAll();
    }

    // View leave statistics
    public Map<String, Object> getLeaveStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long totalRequests = leaveRepository.count();
        long approvedCount = leaveRepository.countByStatus("Approved");
        stats.put("totalRequests", totalRequests);
        stats.put("approvalRate", (double) approvedCount / totalRequests * 100);
        return stats;
    }


    public Leave updateLeaveRequest(UUID id, Leave leaveRequest) {
        // Fetch the leave request by ID
        Leave existingLeave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        // Check if the leave status is "Pending"
        if (!"Pending".equals(existingLeave.getStatus())) {
            throw new IllegalArgumentException("Leave request can only be updated if the status is 'Pending'.");
        }

        // Update the leave details
        existingLeave.setLeaveType(leaveRequest.getLeaveType());
        existingLeave.setStartDate(leaveRequest.getStartDate());
        existingLeave.setEndDate(leaveRequest.getEndDate());
        existingLeave.setReason(leaveRequest.getReason());

        // Save the updated leave request
        return leaveRepository.save(existingLeave);
    }
    public List<Leave> getAllLeaveHistory() {
        // Fetch all leaves that are not pending
        return leaveRepository.findByStatusNot("Pending");
    }
    public void recallLeave(UUID id) {
        Leave leave = getLeaveById(id);

        // Check if the leave request is already recalled
        if ("Recalled".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been recalled.");
        }

        // Check if the leave request is approved
        if (!"Approved".equals(leave.getStatus())) {
            throw new IllegalArgumentException("Only approved leave requests can be recalled.");
        }

        // Update the status to "Recalled"
        leave.setStatus("Recalled");
        leaveRepository.save(leave);
    }
}