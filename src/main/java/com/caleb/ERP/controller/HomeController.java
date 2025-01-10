package com.caleb.ERP.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index"; // This will return the index.html from templates
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard() {
        return "admin-dashboard"; // This will return the admin-dashboard.html from templates
    }

    @GetMapping("/employee-dashboard")
    public String employeeDashboard() {
        return "employee-dashboard"; // This will return the employee-dashboard.html from templates
    }
}