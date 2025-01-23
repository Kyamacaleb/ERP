package com.caleb.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Table(name = "leave") // Updated table name to be more descriptive
@NoArgsConstructor
@AllArgsConstructor
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "leave_id", nullable = false, updatable = false) // Column name for leave ID
    private UUID leaveId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false) // Column name for employee ID
    private Employee employee;

    @Column(name = "leave_type", nullable = false) // Column name for leave type
    private String leaveType; // Sick, Vacation, Paternity/Maternity, Compassionate

    @Column(name = "start_date", nullable = false) // Column name for start date
    private String startDate; // Format: YYYY-MM-DD

    @Column(name = "end_date", nullable = false) // Column name for end date
    private String endDate; // Format: YYYY-MM-DD

    @Column(name = "reason", nullable = false) // Column name for reason
    private String reason;

    @Column(name = "approver_name") // Column name for approver name
    private String approverName; // Name of the approver

    @Column(name = "date_requested", nullable = false) // Column name for date requested
    private String dateRequested; // Format: YYYY-MM-DD

    @Column(name = "status", nullable = false) // Column name for status
    private String status; // Pending, Approved, Rejected, Recalled

    @Column(name = "days_allocated", nullable = false) // Column name for days allocated
    private int daysAllocated; // Total days allocated for the leave type

    @Column(name = "days_taken", nullable = false) // Column name for days taken
    private int daysTaken; // Total days taken

    // Leave balances
    @Column(name = "sick_leave_balance", nullable = false)
    private int sickLeaveBalance = 21; // Default balance for sick leave

    @Column(name = "vacation_leave_balance", nullable = false)
    private int vacationLeaveBalance = 21; // Default balance for vacation leave

    @Column(name = "paternity_leave_balance", nullable = false)
    private int paternityLeaveBalance = 21; // Default balance for paternity leave

    @Column(name = "compassionate_leave_balance", nullable = false)
    private int compassionateLeaveBalance = 21; // Default balance for compassionate leave


}