package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID> {
    List<Leave> findByEmployee(Employee employee); // Method to find leaves by employee
    List<Leave> findByStatusAndLeaveType(String status, String leaveType); // Updated method
    List<Leave> findByStatus(String status);
    List<Leave> findByLeaveType(String leaveType); // Updated method
    long countByStatus(String status);
    List<Leave> findByStatusNot(String status);

    List<Leave> findByEmployeeEmailAndStatus(String employeeEmail, String pending);
}