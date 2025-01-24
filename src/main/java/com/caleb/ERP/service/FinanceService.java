package com.caleb.ERP.service;

import com.caleb.ERP.entity.Finance;
import com.caleb.ERP.repository.FinanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceService {
    @Autowired
    private FinanceRepository financeRepository;

    // Get all finances for a specific employee (excluding deleted)
    public List<Finance> getAllFinancesByEmployee(UUID employeeId) {
        return financeRepository.findAll().stream()
                .filter(finance -> finance.getEmployee().getEmployeeId().equals(employeeId) && !finance.isDeleted())
                .collect(Collectors.toList());
    }

    // Create a new finance record (either requisition or claim)
    public Finance createFinance(Finance finance) {
        LocalDate submittedDate = LocalDate.parse(finance.getDateSubmitted());

        // Validate date based on type
        if ("Requisition".equals(finance.getType()) && submittedDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Past dates are not allowed for requisitions.");
        }

        if ("Claim".equals(finance.getType()) && submittedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Future dates are not allowed for claims.");
        }

        // Validate file type
        if (!isValidFileType(finance.getSupportingDocuments())) {
            throw new IllegalArgumentException("Supporting documents must be in PDF or Word format.");
        }

        finance.setStatus("Pending"); // Default status
        return financeRepository.save(finance);
    }

    // Get a finance record by ID
    public Optional<Finance> getFinanceById(UUID financeId) {
        return financeRepository.findById(financeId);
    }

    // Update an existing finance record
    public Finance updateFinance(UUID financeId, Finance financeDetails) {
        Finance finance = financeRepository.findById(financeId).orElseThrow();
        finance.setPurpose(financeDetails.getPurpose());
        finance.setExpenseType(financeDetails.getExpenseType());
        finance.setAmount(financeDetails.getAmount());
        finance.setDateSubmitted(financeDetails.getDateSubmitted());
        finance.setSupportingDocuments(financeDetails.getSupportingDocuments());
        return financeRepository.save(finance);
    }

    // Soft delete a finance record
    public void deleteFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new IllegalArgumentException("Finance record not found"));
        finance.setDeleted(true); // Mark as deleted
        finance.setDeletedAt(LocalDateTime.now()); // Set deletion timestamp
        financeRepository.save(finance); // Save the updated record
    }

    public void approveFinance(UUID financeId, String feedback) {
        Finance finance = financeRepository.findById(financeId).orElseThrow();

        // Check if the finance record is already approved or rejected
        if ("Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been approved.");
        }
        if ("Rejected".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been rejected.");
        }

        finance.setStatus("Approved");
        finance.setFeedback(feedback);
        financeRepository.save(finance);
    }

    public void rejectFinance(UUID financeId, String feedback) {
        Finance finance = financeRepository.findById(financeId).orElseThrow();

        // Check if the finance record is already approved or rejected
        if ("Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been approved.");
        }
        if ("Rejected".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been rejected.");
        }

        finance.setFeedback(feedback);
        finance.setStatus("Rejected");
        financeRepository.save(finance);
    }

    // Validate file type for supporting documents
    private boolean isValidFileType(String filePath) {
        return filePath.endsWith(".pdf") || filePath.endsWith(".doc") || filePath.endsWith(".docx");
    }

    // Get finance history for a specific employee
    public List<Finance> getFinanceHistoryByEmployee(UUID employeeId) {
        return financeRepository.findAll().stream()
                .filter(finance -> finance.getEmployee().getEmployeeId().equals(employeeId))
                .collect(Collectors.toList());
    }

    public List<Finance> getAllFinances() {
        return financeRepository.findAll();
    }
    public void recallFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new IllegalArgumentException("Finance record not found"));

        // Check if the finance record is approved before recalling
        if (!"Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("Only approved records can be recalled.");
        }

        // Change the status to "Recalled"
        finance.setStatus("Recalled");
        financeRepository.save(finance);
    }

    // Get deleted finances
    public List<Finance> getDeletedFinances() {
        return financeRepository.findAll().stream()
                .filter(Finance::isDeleted)
                .collect(Collectors.toList());
    }

    // Restore a deleted finance record
    public void restoreFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new IllegalArgumentException("Finance record not found"));
        finance.setDeleted(false); // Mark as not deleted
        finance.setDeletedAt(null); // Clear the deletion timestamp
        financeRepository.save(finance); // Save the updated record
    }
}