package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "notification")
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private Employee recipient;

    private String message;

    private LocalDateTime dateSent;

    private boolean readStatus;

    @PrePersist
    public void prePersist() {
        this.readStatus = false; // Default to unread
        this.dateSent = LocalDateTime.now(); // Set the current time
    }
}

