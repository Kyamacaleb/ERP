package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    // Method to find an employee by name
    Optional<Employee> findByFirstNameAndLastName(String firstName, String lastName);
    Optional<Employee> findByRole(String role);
}