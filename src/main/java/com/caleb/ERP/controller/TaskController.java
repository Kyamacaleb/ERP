package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Task;
import com.caleb.ERP.service.EmployeeService;
import com.caleb.ERP.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @Autowired
    private TaskService taskService;
    @Autowired
    private EmployeeService employeeService;

    // Get all tasks
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    // Create a new task
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        // Set the assignedBy and assignedTo fields based on the current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String assignedByEmail = authentication.getName(); // Assuming the email is used as the username
        Employee assignedBy = employeeService.getEmployeeByEmail(assignedByEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Set the assignedBy field
        task.setAssignedBy(assignedBy);

        // You will need to set the assignedTo field based on the request body or logic
        // For example, if you want to assign it to a specific employee:
        // task.setAssignedTo(employeeService.getEmployeeById(assignedToId).orElseThrow(() -> new RuntimeException("Employee not found")));

        Task createdTask = taskService.createTask(task);
        return ResponseEntity.status(201).body(createdTask);
    }

    // Get a task by ID
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable UUID taskId) {
        return taskService.getTaskById(taskId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update a task
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{taskId}")
    public ResponseEntity<Task> updateTask(@PathVariable UUID taskId, @RequestBody Task taskDetails) {
        Task updatedTask = taskService.updateTask(taskId, taskDetails);
        return ResponseEntity.ok(updatedTask);
    }

    // Delete a task
    @PreAuthorize("hasRole('ADMIN')")
    @ DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    // Update Task Status (for Employee)
    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable UUID taskId, @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(null); // Return 400 Bad Request if status is not provided
        }

        Task updatedTask = taskService.updateTaskStatus(taskId, status); // Assuming you have a method to update the task status
        return ResponseEntity.ok(updatedTask);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/{taskId}/comments")
    public ResponseEntity<Void> addComment(@PathVariable UUID taskId, @RequestBody Map<String, String> commentBody) {
        String comment = commentBody.get("comment");
        if (comment == null || comment.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Return 400 Bad Request if comment is not provided
        }
        taskService.addComment(taskId, comment);
        return ResponseEntity.ok().build();
    }
    // Get task history for the current employee
    @GetMapping("/me/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Task>> getEmployeeTaskHistory() {
        List<Task> tasks = taskService.getTasksByCurrentEmployee(); // Implement this method in TaskService
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }
}