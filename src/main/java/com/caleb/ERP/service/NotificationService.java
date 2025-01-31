package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Notification;
import com.caleb.ERP.repository.EmployeeRepository;
import com.caleb.ERP.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void sendNotification(String message, Employee recipient) {
        try {
            // Create a new notification entity
            Notification notification = new Notification();
            notification.setMessage(message);
            notification.setRecipient(recipient);
            notification.setDateSent(LocalDateTime.now());

            // Save the notification to the database
            notificationRepository.save(notification);


            // Notify admin as well (assuming you have a method to get the admin's email or ID)
            String adminMessage = "Notification sent to " + recipient.getFullName() + ": " + message;
            notificationRepository.save(createAdminNotification(adminMessage)); // Save admin notification

        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage());
        }
    }

    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByRecipient(Employee recipient) {
        return notificationRepository.findByRecipient(recipient);
    }

    // Method to save a notification (if needed)
    public Notification save(Notification notification) {
        return notificationRepository.save(notification); // Save or update the notification
    }

    // Helper method to create an admin notification
    private Notification createAdminNotification(String message) {
        Notification adminNotification = new Notification();
        adminNotification.setMessage(message);
        adminNotification.setDateSent(LocalDateTime.now());
        // Set recipient to admin (you need to implement this based on your application logic)
        adminNotification.setRecipient(getAdminEmployee()); // Replace with actual admin retrieval logic
        return adminNotification;
    }

    // Example method to get the admin employee (you need to implement this based on your application logic)
    private Employee getAdminEmployee() {
        return employeeRepository.findByRole("ADMIN")
                .orElseThrow(() -> new NoSuchElementException("Admin employee not found"));
    }
}