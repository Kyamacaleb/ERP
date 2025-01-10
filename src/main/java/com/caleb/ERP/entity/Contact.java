package com.caleb.ERP.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Table(name = "contact")
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contact_id", nullable = false, updatable = false)
    private UUID contactId;

    @OneToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "name", nullable = false)
    private String name;

    @JsonIgnore
    @Column(name = "role")
    private String role;

    @Column(name = "department")
    private String department;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true; // Default to true, meaning the contact is active
}