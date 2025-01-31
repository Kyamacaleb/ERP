package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LeaveBalanceResetService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceResetService.class);

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService; // Inject NotificationService

    // This method will run at 00:00 on January 1st every year
    @Scheduled(cron = "0 0 0 1 1 *") // Cron expression for January 1st at midnight
    public void resetLeaveBalances() {
        List<Employee> employees = employeeService.getAllEmployees(); // Fetch all employees
        for (Employee employee : employees) {
            employee.setSickLeaveBalance(21); // Reset sick leave balance
            employee.setVacationLeaveBalance(21); // Reset vacation leave balance
            employee.setPaternityLeaveBalance(21); // Reset paternity leave balance
            employee.setCompassionateLeaveBalance(21); // Reset compassionate leave balance
            employeeService.saveEmployee(employee); // Save the updated employee

            // Send notification about the leave balance reset for each employee
            String message = "Leave balances have been reset for " + employee.getFullName() + ".";
            notificationService.sendNotification(message, employee); // Notify employee

            // Notify admin
            String adminMessage = "Leave balances have been reset for Employee: " + employee.getFullName();
            notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin
        }

        // Optionally, send a summary notification
        String summaryMessage = "Leave balances have been reset for all employees.";
        notificationService.sendNotification(summaryMessage, null); // Send to no specific recipient

        logger.info("Leave balances have been reset for all employees.");
    }

    // Example method to get the admin employee (you need to implement this based on your application logic)
    private Employee getAdminEmployee() {
        return employeeRepository.findByRole("ADMIN")
                .orElseThrow(() -> new NoSuchElementException("Admin employee not found"));
    }
}