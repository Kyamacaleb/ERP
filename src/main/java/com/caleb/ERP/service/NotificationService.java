package com.caleb.ERP.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper; // For JSON conversion

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
    }

    public void sendAdminNotification(String message) {
        sendNotification("/topic/admins", message);
    }

    public void sendEmployeeNotification(String message) {
        sendNotification("/topic/employees", message);
    }

    private void sendNotification(String destination, String message) {
        Notification notification = new Notification(message); // Create a Notification object
        try {
            String notificationJson = objectMapper.writeValueAsString(notification); // Convert to JSON
            messagingTemplate.convertAndSend(destination, notificationJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Handle JSON processing exception
        }
    }

    // Inner class to represent a notification
    @Getter
    private static class Notification {
        private String message;

        public Notification(String message) {
            this.message = message;
        }

    }
}