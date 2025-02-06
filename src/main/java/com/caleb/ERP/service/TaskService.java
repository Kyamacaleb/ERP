package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Task;
import com.caleb.ERP.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private NotificationService notificationService; // Inject NotificationService

    public List<Task> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        for (Task task : tasks) {
            setTaskNames(task);
        }
        return tasks;
    }

    public Task createTask(Task task) {
        Task createdTask = taskRepository.save(task);

        // Send notification to the assigned employee
        String employeeMessage = "A new task has been assigned to you: " + createdTask.getTaskName();
        notificationService.sendEmployeeNotification(employeeMessage);

        // Optionally, send notification to the admin
        String adminMessage = "A new task has been created: " + createdTask.getTaskName();
        notificationService.sendAdminNotification(adminMessage);

        return createdTask;
    }

    public Optional<Task> getTaskById(UUID taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        taskOpt.ifPresent(this::setTaskNames); // Set names if task is found
        return taskOpt;
    }

    public Task updateTask(UUID taskId, Task taskDetails) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        task.setTaskName(taskDetails.getTaskName());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setUrgent(taskDetails.isUrgent());

        Task updatedTask = taskRepository.save(task);

        // Send notification to the assigned employee
        String employeeMessage = "Your task has been updated: " + updatedTask.getTaskName();
        notificationService.sendEmployeeNotification(employeeMessage);

        // Optionally, send notification to the admin
        String adminMessage = "Task updated: " + updatedTask.getTaskName();
        notificationService.sendAdminNotification(adminMessage);

        return updatedTask;
    }

    public void deleteTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        // Optionally, send notification to the assigned employee before deletion
        String employeeMessage = "Your task has been deleted: " + task.getTaskName();
        notificationService.sendEmployeeNotification(employeeMessage);

        taskRepository.deleteById(taskId);
    }

    public Task updateTaskStatus(UUID taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        // Send notification to the assigned employee
        String employeeMessage = "The status of your task has been updated to: " + updatedTask.getStatus();
        notificationService.sendEmployeeNotification(employeeMessage);

        return updatedTask;
    }

    public List<Task> getTasksByCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        Employee currentEmployee = employeeService.getEmployeeByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<Task> tasks = taskRepository.findByAssignedTo(currentEmployee);
        for (Task task : tasks) {
            setTaskNames(task);
        }
        return tasks;
    }

    private void setTaskNames(Task task) {
        if (task.getAssignedTo() != null) {
            task.setAssignedToName(task.getAssignedTo().getFullName()); // Use getFullName() method
        }
        if (task.getAssignedBy() != null) {
            task.setAssignedByName(task.getAssignedBy().getFullName()); // Use getFullName() method
        }
    }

    public List<Task> getPendingTasksByCurrentEmployee(UUID employeeId) {
        return taskRepository.findByAssignedToEmployeeIdAndStatus(employeeId, "Not Started");
    }


    // Method to validate task name
    public boolean isValidTaskName(String taskName) {
        return taskName != null && taskName.matches("[a-zA-Z0-9\\s\\p{Punct}]+");
    }
    public Task requestExtension(UUID taskId, String reason, String newDueDate) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        if (!task.getStatus().equals("in-progress")) {
            throw new RuntimeException("Extension can only be requested for tasks in progress.");
        }

        LocalDate currentDueDate = LocalDate.parse(task.getDueDate());
        LocalDate proposedNewDueDate = LocalDate.parse(newDueDate);

        // Validate that the new due date is in the future and after the current due date
        if (!proposedNewDueDate.isAfter(currentDueDate)) {
            throw new RuntimeException("The new due date must be in the future and later than the current due date.");
        }

        task.setExtensionRequested(true);
        task.setExtensionReason(reason);
        task.setNewDueDate(newDueDate);
        task.setRequestTimestamp(LocalDateTime.now());

        return taskRepository.save(task);
    }

    public Task approveExtension(UUID taskId, Employee admin) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        if (!task.isExtensionRequested()) {
            throw new RuntimeException("No extension request found for this task.");
        }

        task.setApprovalStatus("Approved");
        task.setApprovalTimestamp(LocalDateTime.now());
        task.setDueDate(task.getNewDueDate()); // Update the due date to the new one
        task.setExtensionRequested(false); // Reset the extension request flag
        task.setApprovedBy(admin); // Set the admin who approved the request

        return taskRepository.save(task);
    }

    public Task rejectExtension(UUID taskId, Employee admin) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        if (!task.isExtensionRequested()) {
            throw new RuntimeException("No extension request found for this task.");
        }

        task.setApprovalStatus("Rejected");
        task.setApprovalTimestamp(LocalDateTime.now());
        task.setExtensionRequested(false); // Reset the extension request flag
        task.setApprovedBy(admin); // Set the admin who rejected the request

        return taskRepository.save(task);
    }

}