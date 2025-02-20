package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FinanceRepository extends JpaRepository<Finance, UUID> {
    List<Finance> findByEmployeeAndStatus(Employee employee, String status);

    List<Finance> findByStatus(String status);
}