package com.caleb.ERP.service;

import com.caleb.ERP.dto.DepartmentStatistics;
import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class EmployeeService implements UserDetailsService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ContactService contactService;

    @Autowired
    private NotificationService notificationService;

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
            admin.setLastName("User  ");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("Admin@123")); // Set a default password
            admin.setRole("ADMIN");
            admin.setSickLeaveBalance(21); // Default sick leave balance
            admin.setVacationLeaveBalance(21); // Default vacation leave balance
            admin.setPaternityLeaveBalance(21); // Default paternity leave balance
            admin.setCompassionateLeaveBalance(21); // Default compassionate leave balance
            admin.setDateOfJoining(LocalDate.now()); // Set default date of joining
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
        // Validate first name, last name, and emergency contact name
        validateEmployeeNames(employee);

        // Validate email format
        if (!isValidEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        // Validate phone number format
        if (!isValidPhoneNumber(employee.getPhoneNumber())) {
            throw new IllegalArgumentException("Invalid phone number format. It should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.");
        }

        // Validate emergency contact phone number format
        if (!isValidPhoneNumber(employee.getEmergencyContactPhone())) {
            throw new IllegalArgumentException("Invalid emergency contact phone number format. It should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.");
        }

        // Validate employment date
        if (employee.getDateOfJoining() != null && !isValidEmploymentDate(employee.getDateOfJoining())) {
            throw new IllegalArgumentException("Date of joining must not be in the future.");
        }

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
        // Check if a contact already exists for the new employee
        Optional<Contact> existingContactOpt = contactService.getContactByEmployee(savedEmployee);
        if (existingContactOpt.isEmpty()) {
            // Create contact for the new employee only if it doesn't exist
            contactService.createContactForEmployee(savedEmployee);
        } else {
            // Optionally, you can update the existing contact if needed
            contactService.updateContactForEmployee(savedEmployee);
        }

        // Send notification to admins
        String adminMessage = String.format("A new employee has been successfully created: %s (Email: %s, Role: %s). Please welcome them to the team!",
                savedEmployee.getFullName(), savedEmployee.getEmail(), savedEmployee.getRole());
        notificationService.sendAdminNotification(adminMessage);

        return savedEmployee;
    }

    public Employee updateEmployee(UUID id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Employee not found"));

        // Validate first name, last name, and emergency contact name
        validateEmployeeNames(employeeDetails);

        // Validate phone number format
        if (!isValidPhoneNumber(employeeDetails.getPhoneNumber())) {
            throw new IllegalArgumentException("Invalid phone number format. It should start with +2547, 07, or 011 followed by 8 digits.");
        }

        // Validate emergency contact phone number format
        if (!isValidPhoneNumber(employeeDetails.getEmergencyContactPhone())) {
            throw new IllegalArgumentException("Invalid emergency contact phone number format. It should start with +2547, 07, or 011 followed by 8 digits.");
        }

        // Validate employment date
        if (employeeDetails.getDateOfJoining() != null && !isValidEmploymentDate(employeeDetails.getDateOfJoining())) {
            throw new IllegalArgumentException("Date of joining must not be in the future.");
        }

        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setEmergencyContactName(employeeDetails.getEmergencyContactName());
        employee.setEmergencyContactPhone(employeeDetails.getEmergencyContactPhone());
        employee.setDepartment(employeeDetails.getDepartment());
        employee.setDateOfJoining(employeeDetails.getDateOfJoining()); // Update date of joining

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

        // Save the updated employee
        Employee updatedEmployee = employeeRepository.save(employee);

        // Send notification to admins
        String adminMessage = String.format("Employee details have been updated: %s (Email: %s). Please review the changes.",
                updatedEmployee.getFullName(), updatedEmployee.getEmail());
        notificationService.sendAdminNotification(adminMessage);

// Send notification to the employee
        String employeeMessage = String.format("Your details have been updated successfully, %s. If you notice any discrepancies, please contact HR.",
                updatedEmployee.getFullName());
        notificationService.sendEmployeeNotification(employeeMessage);

        return updatedEmployee;
    }

    private void validateEmployeeNames(Employee employee) {
        // Validate first name, last name, and emergency contact name
        if (!isAlphabetic(employee.getFirstName())) {
            throw new IllegalArgumentException("First name must contain only alphabetic characters.");
        }
        if (!isAlphabetic(employee.getLastName())) {
            throw new IllegalArgumentException("Last name must contain only alphabetic characters.");
        }
        if (!isAlphabetic(employee.getEmergencyContactName())) {
            throw new IllegalArgumentException("Emergency contact name must contain only alphabetic characters.");
        }
    }

    private boolean isAlphabetic(String str) {
        return str != null && str.matches("^[A-Za-z\\s'-]+$");
    }

    public boolean existsByEmail(String email) {
        return employeeRepository.findAll().stream()
                .anyMatch(employee -> employee.getEmail().equals(email));
    }

    public boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String phoneRegex = "^(\\+254(10[0-9]|11[0-9]|7[0-9]{8})|07[0-9]{8}|011[0-9]{8})$"; // Matches +25410x, +25411x, +2547xx, 07XX, 010x, 011x
        return Pattern.matches(phoneRegex, phoneNumber);
    }

    private boolean isValidEmploymentDate(LocalDate employmentDate) {
        LocalDate today = LocalDate.now();
        return !employmentDate.isAfter(today); // Employment date should not be in the future
    }

    public boolean isValidPassword(String password) {
        // Example: At least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }

    public List<Employee> getDeactivatedEmployees() {
        return employeeRepository.findAll().stream()
                .filter(employee -> !employee.isActive())
                .toList();
    }

    public void deactivateEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Employee not found"));
        employee.setActive(false); // Set the active status to false
        employeeRepository.save(employee);
        contactService.deactivateContactForEmployee(employee); // Deactivate the associated contact

        // Send notification to admins
        String adminMessage = String.format("Employee has been deactivated: %s (Email: %s). Please ensure all necessary actions are taken.",
                employee.getFullName(), employee.getEmail());
        notificationService.sendAdminNotification(adminMessage);
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

    public void resetEmployeePasswordByEmail(String email, String newPassword) {
        // Validate the email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        // Validate the new password
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Password does not meet complexity requirements.");
        }

        // Find the employee by email
        Employee employee = (Employee) employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));

        // Hash the new password
        employee.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated employee
        employeeRepository.save(employee);

        // Send notification to admins
        String adminMessage = String.format("The password for employee %s has been reset. Please ensure they are informed of the new password.",
                employee.getFullName());
        notificationService.sendAdminNotification(adminMessage);

// Send notification to the employee
        String employeeMessage = String.format("Hello %s, your password has been reset. Please check your email for instructions on how to set a new password.",
                employee.getFullName());
        notificationService.sendEmployeeNotification(employeeMessage);
    }

    public void changePassword(UUID employeeId, String currentPassword, String newPassword) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));

        // Validate the current password
        if (!passwordEncoder.matches(currentPassword, employee.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // Validate the new password
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password does not meet complexity requirements.");
        }

        // Hash the new password
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        // Send notification to the employee
        String employeeMessage = String.format("Hi %s, your password has been changed successfully. If you did not make this change, please contact support immediately.",
                employee.getFullName());
        notificationService.sendEmployeeNotification(employeeMessage);
    }

    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }
    public List<DepartmentStatistics> getEmployeeCountByDepartment() {
        return employeeRepository.countEmployeesByDepartment();
    }
}