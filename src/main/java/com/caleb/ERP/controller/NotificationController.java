package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Notification;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.service.NotificationService;
import com.caleb.ERP.service.EmployeeService; // Ensure you have this service for employee retrieval
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final EmployeeService employeeService;

    @Autowired
    public NotificationController(NotificationService notificationService, EmployeeService employeeService) {
        this.notificationService = notificationService;
        this.employeeService = employeeService;
    }

    @MessageMapping("/send")
    @SendTo("/topic/notifications")
    public String send(@RequestParam String message, @RequestParam UUID recipientId) {
        Employee recipient = employeeService.getEmployeeById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        notificationService.sendNotification(message, recipient);
        return message;
    }

    // Endpoint for employee notifications
    @GetMapping("/employee")
    public List<Notification> getNotificationsForEmployee() {
        UUID currentUserId = getCurrentUserId(); // Get the current user's ID
        Employee currentUser  = employeeService.getEmployeeById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        return notificationService.getNotificationsByRecipient(currentUser );
    }

    // Endpoint for admin notifications
    @GetMapping("/admin")
    public List<Notification> getNotificationsForAdmin() {
        // Assuming you have a method to get the current authenticated user
        UUID currentUserId = getCurrentUserId();
        Employee currentUser  = employeeService.getEmployeeById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Check if the current user is an admin
        if (!"ADMIN".equals(currentUser .getRole())) {
            throw new RuntimeException("Access denied: Only admins can view this resource.");
        }

        // Fetch notifications meant for admins
        return notificationService.getNotificationsByRecipient(currentUser ); // Adjust this method to filter admin notifications
    }

    @PostMapping("/mark-as-read/{notificationId}")
    public void markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @PostMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllAsRead() {
        UUID currentUserId = getCurrentUserId(); // Get the current user's ID

        // Fetch the Employee object using the current user's ID
        Employee currentUser  = employeeService.getEmployeeById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Fetch all notifications for the current user
        List<Notification> notifications = notificationService.getNotificationsByRecipient(currentUser );

        // Mark each notification as read
        for (Notification notification : notifications) {
            notification.setReadStatus(true);
            notificationService.save(notification); // Use the service to save the notification
        }

        return ResponseEntity.ok().build(); // Return a 200 OK response
    }

    // Method to get the current user's ID
    private UUID getCurrentUserId() {
        // Assuming you are using Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName(); // Get the email or username
            Employee employee = employeeService.getEmployeeByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User  not found"));
            return employee.getEmployeeId(); // Return the employee ID
        }
        throw new RuntimeException("User  not authenticated");
    }
}