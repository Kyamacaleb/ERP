package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Table(name = "finance")
@AllArgsConstructor
@NoArgsConstructor
public class Finance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID financeId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "type", nullable = false)
    private String type; // "Requisition" or "Claim"

    @Column(name = "purpose", nullable = false)
    private String purpose; // For requisitions

    @Column(name = "expense_type")
    private String expenseType; // For claims

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "date_submitted", nullable = false)
    private String dateSubmitted; // Format: YYYY-MM-DD

    @Column(name = "supporting_documents", nullable = false)
    private String supportingDocuments; // Path to the uploaded document

    @Column(name = "status", nullable = false)
    private String status; // e.g., "Pending", "Approved", "Rejected"

    @Column(name = "feedback") // New field for feedback
    private String feedback; // Feedback from admin
}