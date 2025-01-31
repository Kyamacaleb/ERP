package com.caleb.ERP.repository;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipient(Employee recipient);
    List<Notification> findByReadStatus(boolean readStatus);
}