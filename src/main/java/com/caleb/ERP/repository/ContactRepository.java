package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Contact findByEmployee(Employee employee);
}