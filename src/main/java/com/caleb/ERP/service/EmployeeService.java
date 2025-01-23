package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeService implements UserDetailsService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ContactService contactService; // Inject ContactService

    @PostConstruct
    public void init() {
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        // Check if an admin account exists
        if (employeeRepository.findAll().stream().noneMatch(e -> e.getRole().equals("ADMIN"))) {
            // Create a default admin account
            Employee admin = new Employee();
            admin.setFirstName("Admin");
            admin.setLastName("User ");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("Admin@123")); // Set a default password
            admin.setRole("ADMIN");
            admin.setSickLeaveBalance(21); // Default sick leave balance
            admin.setVacationLeaveBalance(21); // Default vacation leave balance
            admin.setPaternityLeaveBalance(21); // Default paternity leave balance
            admin.setCompassionateLeaveBalance(21); // Default compassionate leave balance
            employeeRepository.save(admin);
        }
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(UUID id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findAll().stream()
                .filter(employee -> employee.getEmail().equals(email))
                .findFirst();
    }

    public Employee createEmployee(Employee employee) {
        // Check password validity
        if (!isValidPassword(employee.getPassword())) {
            throw new IllegalArgumentException("Password does not meet complexity requirements.");
        }

        // Check if the email already exists
        if (existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email already exists. Please use a different email.");
        }

        // Hash the password
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setActive(true); // Set active by default

        // Ensure a role is assigned
        if (employee.getRole() == null || employee.getRole().isEmpty()) {
            employee.setRole("EMPLOYEE"); // Default role
        }

        // Save the employee
        Employee savedEmployee = employeeRepository.save(employee);
        // Create contact for the new employee
        contactService.createContactForEmployee(savedEmployee);
        return savedEmployee;
    }

    public Employee updateEmployee(UUID id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setEmergencyContactName(employeeDetails.getEmergencyContactName());
        employee.setEmergencyContactPhone(employeeDetails.getEmergencyContactPhone());
        employee.setDepartment(employeeDetails.getDepartment());
        // Optionally update leave balances if provided
        if (employeeDetails.getSickLeaveBalance() != null) {
            employee.setSickLeaveBalance(employeeDetails.getSickLeaveBalance());
        }
        if (employeeDetails.getVacationLeaveBalance() != null) {
            employee.setVacationLeaveBalance(employeeDetails.getVacationLeaveBalance());
        }
        if (employeeDetails.getPaternityLeaveBalance() != null) {
            employee.setPaternityLeaveBalance(employeeDetails.getPaternityLeaveBalance());
        }
        if (employeeDetails.getCompassionateLeaveBalance() != null) {
            employee.setCompassionateLeaveBalance(employeeDetails.getCompassionateLeaveBalance());
        }
        // Update the contact information
        contactService.updateContactForEmployee(employee);
        return employeeRepository.save(employee);
    }

    public void deactivateEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        employee.setActive(false); // Set the active status to false
        employeeRepository.save(employee);
        contactService.deactivateContactForEmployee(employee); // Deactivate the associated contact
    }

    public boolean existsByEmail(String email) {
        return employeeRepository.findAll().stream()
                .anyMatch(employee -> employee.getEmail().equals(email));
    }


    public List<Employee> getDeactivatedEmployees() {
        return employeeRepository.findAll().stream()
                .filter(employee -> !employee.isActive())
                .toList();
    }

    public boolean isValidPassword(String password) {
        // Example: At least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = getEmployeeByEmail(username).orElseThrow(() ->
                new UsernameNotFoundException("User  not found with email: " + username));

        if (!employee.isActive()) {
            throw new UsernameNotFoundException("User  is deactivated and cannot log in");
        }

        String role = employee.getRole(); // it returns "ROLE_EMPLOYEE"

        return org.springframework.security.core.userdetails.User.builder()
                .username(employee.getEmail()) // Assuming the email field is used for login
                .password(employee.getPassword()) // Assuming the password field is used for login
                .roles(role.replace("ROLE_", "")) // Remove "ROLE_" prefix
                .build();
    }

    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

}