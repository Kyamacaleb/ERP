package com.caleb.ERP.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class NotificationService {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper; // For JSON conversion

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
        this.objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule

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
            System.out.println("Notification JSON: " + notificationJson); // Log the JSON
            messagingTemplate.convertAndSend(destination, notificationJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Handle JSON processing exception
        }
    }

    // Inner class to represent a notification
    @Getter
    private static class Notification {
        private String message;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "UTC")
        private LocalDateTime timestamp; // Keep LocalDateTime

        public Notification(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now(); // Set the timestamp to the current time
        }
}}