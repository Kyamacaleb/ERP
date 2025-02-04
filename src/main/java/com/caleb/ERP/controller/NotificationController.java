package com.caleb.ERP.controller;

import com.caleb.ERP.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Endpoint to send a notification to admins
    @PostMapping("/admin")
    public ResponseEntity<String> sendAdminNotification(@RequestBody String message) {
        notificationService.sendAdminNotification(message);
        return ResponseEntity.ok("Notification sent to admins: " + message);
    }

    // Endpoint to send a notification to employees
    @PostMapping("/employee")
    public ResponseEntity<String> sendEmployeeNotification(@RequestBody String message) {
        notificationService.sendEmployeeNotification(message);
        return ResponseEntity.ok("Notification sent to employees: " + message);
    }
}