package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate; // Import LocalDate
import java.util.UUID;

@Data
@Entity
@Table(name = "employee")
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "role", nullable = false)
    private String role; // This will store roles like ADMIN, EMPLOYEE, or DEACTIVATED

    @Column(name = "department")
    private String department;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining; // Change to LocalDate

    @Column(name = "profile_picture")
    private String profilePicture; // Field for profile picture

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "active", nullable = false)
    private boolean active = true; // Default to true, meaning the employee is active

    public boolean isActive() {
        return active;
    }

    // Leave balances
    @Column(name = "sick_leave_balance", nullable = false)
    private Integer sickLeaveBalance = 21; // Default balance for sick leave

    @Column(name = "vacation_leave_balance", nullable = false)
    private Integer vacationLeaveBalance = 21; // Default balance for vacation leave

    @Column(name = "paternity_leave_balance", nullable = false)
    private Integer paternityLeaveBalance = 21; // Default balance for paternity leave

    @Column(name = "compassionate_leave_balance", nullable = false)
    private Integer compassionateLeaveBalance = 21; // Default balance for compassionate leave

    public String getFullName() {
        return firstName + " " + lastName;
    }
}