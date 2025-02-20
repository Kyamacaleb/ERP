package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Finance;
import com.caleb.ERP.repository.FinanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    @Autowired
    private FinanceRepository financeRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private NotificationService notificationService; // Inject NotificationService

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

        // Validate file type only for claims
        if ("Claim".equals(finance.getType()) && !isValidFileType(finance.getSupportingDocuments())) {
            throw new IllegalArgumentException("Supporting documents must be in PDF or Word format.");
        }

        finance.setStatus("Pending"); // Default status
        Finance savedFinance = financeRepository.save(finance);

        // Send notification to admins
        String adminMessage = String.format("A new finance record has been created by %s: %s (Amount: KES %.2f, Type: %s). Please review it.",
                savedFinance.getEmployee().getFullName(), savedFinance.getPurpose(), savedFinance.getAmount(), savedFinance.getType());
        notificationService.sendAdminNotification(adminMessage);

        // Send notification to employee
        String employeeMessage = String.format("Your finance record has been successfully created: %s (Amount: KES %.2f). It is currently pending approval.",
                savedFinance.getPurpose(), savedFinance.getAmount());
        notificationService.sendEmployeeNotification(employeeMessage);

        return savedFinance;
    }

    // Get a finance record by ID
    public Optional<Finance> getFinanceById(UUID financeId) {
        return financeRepository.findById(financeId);
    }

    // Update an existing finance record
    public Finance updateFinance(UUID financeId, Finance financeDetails) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        finance.setPurpose(financeDetails.getPurpose());
        finance.setExpenseType(financeDetails.getExpenseType());
        finance.setAmount(financeDetails.getAmount());
        finance.setDateSubmitted(financeDetails.getDateSubmitted());
        finance.setSupportingDocuments(financeDetails.getSupportingDocuments());

        Finance updatedFinance = financeRepository.save(finance);

        // Send notification to admins
        String adminMessage = String.format("The finance record for %s has been updated: %s (New Amount: KES %.2f). Please review the changes.",
                updatedFinance.getEmployee().getFullName(), updatedFinance.getPurpose(), updatedFinance.getAmount());
        notificationService.sendAdminNotification(adminMessage);

        // Send notification to employee
        String employeeMessage = String.format("Your finance record has been updated successfully: %s (New Amount: KES %.2f).",
                updatedFinance.getPurpose(), updatedFinance.getAmount());
        notificationService.sendEmployeeNotification(employeeMessage);

        return updatedFinance;
    }

    // Soft delete a finance record
    public void deleteFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        finance.setDeleted(true); // Mark as deleted
        finance.setDeletedAt(LocalDateTime.now()); // Set deletion timestamp
        financeRepository.save(finance); // Save the updated record

        // Send notification to admins
        String adminMessage = String.format("The finance record for %s has been marked as deleted: %s (Amount: KES %.2f).",
                finance.getEmployee().getFullName(), finance.getPurpose(), finance.getAmount());
        notificationService.sendAdminNotification(adminMessage);

        // Send notification to employee
        String employeeMessage = String.format("Your finance record has been marked as deleted: %s (Amount: KES %.2f). If this was a mistake, please contact support.",
                finance.getPurpose(), finance.getAmount());
        notificationService.sendEmployeeNotification(employeeMessage);
    }

    public void approveFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        // Check if the finance record is already approved or rejected
        if ("Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been approved.");
        }
        if ("Rejected".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been rejected.");
        }

        finance.setStatus("Approved");
        financeRepository.save(finance);

        // Send notification to admins
        String adminMessage = String.format("The finance record for %s has been approved: %s (Amount: KES %.2f).",
                finance.getEmployee().getFullName(), finance.getPurpose(), finance.getAmount());
        notificationService.sendAdminNotification(adminMessage);

// Send notification to employee
        String employeeMessage = String.format("Congratulations! Your finance record has been approved: %s (Amount: KES %.2f). You can now proceed with your plans.",
                finance.getPurpose(), finance.getAmount());
        notificationService.sendEmployeeNotification(employeeMessage);
    }

    public void rejectFinance(UUID financeId) {
        Finance finance = financeRepository.findById(financeId)
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        // Check if the finance record is already approved or rejected
        if ("Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been approved.");
        }
        if ("Rejected".equals(finance.getStatus())) {
            throw new IllegalArgumentException("This finance record has already been rejected.");
        }

        finance.setStatus("Rejected");
        financeRepository.save(finance);

        // Send notification to admins
        String adminMessage = String.format("The finance record for %s has been rejected: %s (Amount: KES %.2f).",
                finance.getEmployee().getFullName(), finance.getPurpose(), finance.getAmount());
        notificationService.sendAdminNotification(adminMessage);

// Send notification to employee
        String employeeMessage = String.format("Unfortunately, your finance record has been rejected: %s (Amount: KES %.2f). Please contact the admin.",
                finance.getPurpose(), finance.getAmount());
        notificationService.sendEmployeeNotification(employeeMessage);
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
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        // Check if the finance record is approved before recalling
        if (!"Approved".equals(finance.getStatus())) {
            throw new IllegalArgumentException("Only approved records can be recalled.");
        }

        // Change the status to "Recalled"
        finance.setStatus("Recalled");
        financeRepository.save(finance);

        // Send notification to admins
        String adminMessage = "Finance record recalled: " + finance.getPurpose();
        notificationService.sendAdminNotification(adminMessage);

        // Send notification to employee
        String employeeMessage = "Your finance record has been recalled: " + finance.getPurpose();
        notificationService.sendEmployeeNotification(employeeMessage);
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
                .orElseThrow(() -> new NoSuchElementException("Finance record not found"));

        finance.setDeleted(false); // Mark as not deleted
        finance.setDeletedAt(null); // Clear the deletion timestamp
        financeRepository.save(finance); // Save the updated record

        // Send notification to admins
        String adminMessage = String.format("The finance record for %s has been restored: %s (Amount: KES %.2f).",
                finance.getEmployee().getFullName(), finance.getPurpose(), finance.getAmount());
        notificationService.sendAdminNotification(adminMessage);

    }

    public List<Finance> getPendingFinancesByCurrentEmployee(UUID employeeId) {
        // Fetch the employee entity using the employeeId
        Employee employee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));

        // Now use the employee object to find pending finances
        return financeRepository.findByEmployeeAndStatus(employee, "Pending");
    }
    public List<Finance> filterFinances(String status) {
        if (status != null) {
            return financeRepository.findByStatus(status);
        }
        return financeRepository.findAll();
    }
}