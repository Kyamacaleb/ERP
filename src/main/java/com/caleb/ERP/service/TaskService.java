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


    public List<Task> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        for (Task task : tasks) {
            if (task.getAssignedTo() != null) {
                task.setAssignedToName(task.getAssignedTo().getFullName()); // Use getFullName() method
            }
            if (task.getAssignedBy() != null) {
                task.setAssignedByName(task.getAssignedBy().getFullName()); // Use getFullName() method
            }
        }
        return tasks;
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


    public List<Task> getTasksByCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        Employee currentEmployee = employeeService.getEmployeeByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<Task> tasks = taskRepository.findByAssignedTo(currentEmployee);
        for (Task task : tasks) {
            if (task.getAssignedTo() != null) {
                task.setAssignedToName(task.getAssignedTo().getFullName()); // Use getFullName() method
            }
            if (task.getAssignedBy() != null) {
                task.setAssignedByName(task.getAssignedBy().getFullName()); // Use getFullName() method
            }
        }
        return tasks;
    }

}