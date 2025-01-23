package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveBalanceResetService {

    @Autowired
    private EmployeeService employeeService;

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
        }
        System.out.println("Leave balances have been reset for all employees.");
    }
}