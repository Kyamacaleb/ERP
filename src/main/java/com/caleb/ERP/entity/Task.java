package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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


}