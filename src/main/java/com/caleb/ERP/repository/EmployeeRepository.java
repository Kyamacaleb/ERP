package com.caleb.ERP.repository;

import com.caleb.ERP.dto.DepartmentStatistics;
import com.caleb.ERP.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    // Method to find an employee by name
    Optional<Employee> findByFirstNameAndLastName(String firstName, String lastName);
    Optional<Employee> findByRole(String role);
    Optional<Object> findByEmail(String email);

    @Query("SELECT new com.caleb.ERP.dto.DepartmentStatistics(e.department, COUNT(e)) " +
            "FROM Employee e GROUP BY e.department")
    List<DepartmentStatistics> countEmployeesByDepartment();
}