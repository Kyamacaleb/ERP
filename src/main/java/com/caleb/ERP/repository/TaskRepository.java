package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByAssignedTo(Employee assignedTo);
}
