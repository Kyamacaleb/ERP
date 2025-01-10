package com.caleb.ERP.service;

import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.entity.Task;
import com.caleb.ERP.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EmployeeService employeeService;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public Optional<Task> getTaskById(UUID taskId) {
        return taskRepository.findById(taskId);
    }

    public Task updateTask(UUID taskId, Task taskDetails) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setTaskName(taskDetails.getTaskName());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());
        task.setComments(taskDetails.getComments());
        task.setUrgent(taskDetails.isUrgent());
        return taskRepository.save(task);
    }

    public void deleteTask(UUID taskId) {
        taskRepository.deleteById(taskId);
    }

    public Task updateTaskStatus(UUID taskId, String status) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(status);
        taskRepository.save(task);
        return task;
    }

    public void addComment(UUID taskId, String comment) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setComments(task.getComments() + "\n" + comment); // Append new comment
        taskRepository.save(task);
    }
    public List<Task> getTasksByCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName(); // Assuming the email is used as the username
        Employee currentEmployee = employeeService.getEmployeeByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        return taskRepository.findByAssignedTo(currentEmployee); // You need to create this method in TaskRepository
    }
}