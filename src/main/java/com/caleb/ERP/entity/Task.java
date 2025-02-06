package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "task")
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID taskId;

    @ManyToOne
    @JoinColumn(name = "assigned_to", nullable = false)
    private Employee assignedTo; // Employee to whom the task is assigned

    @ManyToOne
    @JoinColumn(name = "assigned_by", nullable = false)
    private Employee assignedBy; // Employee who assigns the task

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "due_date", nullable = false)
    private String dueDate;

    @Column(name = "status", nullable = false)
    private String status; // e.g., "Not Started", "In Progress", "Completed"

    @Column(name = "urgent")
    private boolean urgent; // Flag for urgent tasks

    // Transient fields for UI display
    @Transient
    private String assignedToName; // Name of the employee assigned to the task

    @Transient
    private String assignedByName; // Name of the employee who assigned the task

    @Column(name = "extension_requested")
    private boolean extensionRequested; // Flag to indicate if an extension has been requested

    @Column(name = "extension_reason")
    private String extensionReason; // Reason for the extension request

    @Column(name = "new_due_date")
    private String newDueDate; // New due date if the extension is approved

    @Column(name = "request_timestamp")
    private LocalDateTime requestTimestamp; // Timestamp when the extension was requested

    @Column(name = "approval_timestamp")
    private LocalDateTime approvalTimestamp; // Timestamp when the request was approved or rejected

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Employee approvedBy; // Employee who approved the extension request

    @Column(name = "approval_status")
    private String approvalStatus; // e.g., "Approved", "Rejected"

}