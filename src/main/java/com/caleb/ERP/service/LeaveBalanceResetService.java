package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class LeaveBalanceResetService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceResetService.class);

    @Autowired
    private EmployeeService employeeService;

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

            // Send notification to the employee
            String employeeMessage = "Your leave balances have been reset to 21 days for each type.";
            notificationService.sendEmployeeNotification(employeeMessage);

            // Optionally, send a notification to admins
            String adminMessage = "Leave balances have been reset for employee: " + employee.getFullName();
            notificationService.sendAdminNotification(adminMessage);
        }

        logger.info("Leave balances have been reset for all employees.");
    }
}