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
import java.util.List;
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

    public void approveLeave(UUID id) {
        Leave leave = getLeaveById(id);

        // Check if the leave request is already approved or rejected
        if ("Approved".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been approved.");
        }
        if ("Rejected".equals(leave.getStatus())) {
            throw new IllegalArgumentException("This leave request has already been rejected.");
        }
        Employee employee = leave.getEmployee(); // Get the employee associated with the leave request

        // Check if the employee has enough leave balance
        if (leave.getDaysTaken() > employee.getSickLeaveBalance() && leave.getLeaveType().equals("Sick")) {
            throw new IllegalArgumentException("Insufficient sick leave balance.");
        } else if (leave.getDaysTaken() > employee.getVacationLeaveBalance() && leave.getLeaveType().equals("Vacation")) {
            throw new IllegalArgumentException("Insufficient vacation leave balance.");
        } else if (leave.getDaysTaken() > employee.getPaternityLeaveBalance() && leave.getLeaveType().equals("Paternity/Maternity")) {
            throw new IllegalArgumentException("Insufficient paternity leave balance.");
        } else if (leave.getDaysTaken() > employee.getCompassionateLeaveBalance() && leave.getLeaveType().equals("Compassionate")) {
            throw new IllegalArgumentException("Insufficient compassionate leave balance.");
        }

        // Deduct the leave days from the employee's balance
        switch (leave.getLeaveType()) {
            case "Sick":
                employee.setSickLeaveBalance(employee.getSickLeaveBalance() - leave.getDaysTaken());
                break;
            case "Vacation":
                employee.setVacationLeaveBalance(employee.getVacationLeaveBalance() - leave.getDaysTaken());
                break;
            case "Paternity/Maternity":
                employee.setPaternityLeaveBalance(employee.getPaternityLeaveBalance() - leave.getDaysTaken());
                break;
            case "Compassionate":
                employee.setCompassionateLeaveBalance(employee.getCompassionateLeaveBalance() - leave.getDaysTaken());
                break;
            default:
                throw new IllegalArgumentException("Invalid leave type.");
        }

        // Set the approver name to the current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        leave.setApproverName(authentication.getName()); // Set the approver name

        leave.setStatus("Approved");
        leaveRepository.save(leave);

        // Update the employee's leave balance in the database
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

    public Leave updateLeaveRequest(UUID id, Leave leaveRequest) {
        Leave existingLeave = getLeaveById(id);

        // Update the existing leave request with new details
        existingLeave.setLeaveType(leaveRequest.getLeaveType());
        existingLeave.setStartDate(leaveRequest.getStartDate());
        existingLeave.setEndDate(leaveRequest.getEndDate());
        existingLeave.setReason(leaveRequest.getReason());
        existingLeave.setStatus("Pending"); // Set status to pending for the new request

        // Calculate the number of leave days for the updated request
        LocalDate startDate = LocalDate.parse(leaveRequest.getStartDate());
        LocalDate endDate = LocalDate.parse(leaveRequest.getEndDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Include the end date
        existingLeave.setDaysTaken((int) daysBetween);
        existingLeave.setDaysAllocated(21); // Set the allocated days (constant value)

        // Set the date requested to the current date
        existingLeave.setDateRequested(LocalDate.now().toString()); // Set to current date or leaveRequest.getDateRequested() if provided

        // Save the updated leave request
        return leaveRepository.save(existingLeave);
    }
}