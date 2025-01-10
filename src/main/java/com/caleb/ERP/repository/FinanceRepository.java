package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Finance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FinanceRepository extends JpaRepository<Finance, UUID> {
}