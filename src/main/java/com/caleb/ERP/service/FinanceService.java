package com.caleb.ERP.service;

import com.caleb.ERP.entity.Finance;
import com.caleb.ERP.repository.FinanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceService {
    @Autowired
    private FinanceRepository financeRepository;

    // Get all finances for a specific employee
    public List<Finance> getAllFinancesByEmployee(UUID employeeId) {
        return financeRepository.findAll().stream()
                .filter(finance -> finance.getEmployee().getEmployeeId().equals(employeeId))
                .collect(Collectors.toList());
    }

    // Create a new finance record (either requisition or claim)
    public Finance createFinance(Finance finance) {
        LocalDate submittedDate = LocalDate.parse(finance.getDateSubmitted());

        // Validate date based on type
        if (finance.getType().equals("Requisition") && submittedDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Past dates are not allowed for requisitions.");
        }

        if (finance.getType().equals("Claim") && submittedDate.isAfter(LocalDate.now())) {
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

    // Delete a finance record
    public void deleteFinance(UUID financeId) {
        financeRepository.deleteById(financeId);
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

        finance.setStatus("Rejected");
        finance.setFeedback(feedback);
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
}