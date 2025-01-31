package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Task;
import com.caleb.ERP.repository.EmployeeRepository;
import com.caleb.ERP.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private EmployeeRepository employeeRepository;

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

        // Send notification about the new task
        String message = "New Task Created: " + createdTask.getTaskName() + " has been assigned.";
        notificationService.sendNotification(message, task.getAssignedTo()); // Notify employee

        // Notify admin
        String adminMessage = "New Task Created for Employee: " + task.getAssignedTo().getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin

        return createdTask;
    }


    public Optional<Task> getTaskById(UUID taskId) {
        return taskRepository.findById(taskId);
    }

    public Task updateTask(UUID taskId, Task taskDetails) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        task.setTaskName(taskDetails.getTaskName());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());
        task.setUrgent(taskDetails.isUrgent());

        // Send notification about the task update
        String message = "Task Updated: " + task.getTaskName() + " has been updated.";
        notificationService.sendNotification(message, task.getAssignedTo()); // Notify employee

        // Notify admin
        String adminMessage = "Task Updated for Employee: " + task.getAssignedTo().getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin

        return taskRepository.save(task);
    }

    public void deleteTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        taskRepository.deleteById(taskId);

        // Send notification about the task deletion
        String message = "Task Deleted: " + task.getTaskName() + " has been deleted.";
        notificationService.sendNotification(message, task.getAssignedTo()); // Notify employee

        // Notify admin
        String adminMessage = "Task Deleted for Employee: " + task.getAssignedTo().getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin
    }

    public Task updateTaskStatus(UUID taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));

        task.setStatus(status);

        // Send notification about the task status update
        String message = "Task Status Updated: " + task.getTaskName() + " status has been updated to " + status + ".";
        notificationService.sendNotification(message, task.getAssignedTo()); // Notify employee

        // Notify admin
        String adminMessage = "Task Status Updated for Employee: " + task.getAssignedTo().getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin

        return taskRepository.save(task);
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

    private Employee getAdminEmployee() {
        return employeeRepository.findByRole("ADMIN")
                .orElseThrow(() -> new NoSuchElementException("Admin employee not found"));
    }
}