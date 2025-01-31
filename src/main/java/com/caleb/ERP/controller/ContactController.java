package com.caleb.ERP.controller;

import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.service.ContactService;
import com.caleb.ERP.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    @Autowired
    private ContactService contactService;

    @Autowired
    private EmployeeService employeeService;

    // Get all contacts for ADMIN and EMPLOYEE
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<Contact>> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            logger.error("Error retrieving contacts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // Get contact by employee ID
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/{employeeId}")
    public ResponseEntity<Contact> getContactByEmployeeId(@PathVariable UUID employeeId) {
        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeById(employeeId);
            if (employeeOpt.isPresent()) {
                Optional<Contact> contactOpt = contactService.getContactByEmployee(employeeOpt.get());
                if (contactOpt.isPresent()) {
                    return ResponseEntity.ok(contactOpt.get());
                }
                return ResponseEntity.notFound().build(); // Return 404 if contact is not found
            }
            return ResponseEntity.notFound().build(); // Return 404 if employee is not found
        } catch (Exception e) {
            logger.error("Error retrieving contact for employee ID {}: {}", employeeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // Update contact information for an employee
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping("/{employeeId}")
    public ResponseEntity<Contact> updateContact(@PathVariable UUID employeeId, @RequestBody Contact contactDetails) {
        Optional<Employee> employeeOpt = employeeService.getEmployeeById(employeeId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            contactDetails.setEmployee(employee);
            Contact updatedContact = contactService.updateContactForEmployee(employee);
            return ResponseEntity.ok(updatedContact);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<Contact> createContact(@RequestBody Contact contact) {
        try {
            Contact createdContact = contactService.createContact(contact);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContact);
        } catch (Exception e) {
            logger.error("Error creating contact: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}