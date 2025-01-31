package com.caleb.ERP.service;

import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.ContactRepository;
import com.caleb.ERP.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private NotificationService notificationService; // Inject NotificationService

    public Contact createContactForEmployee(Employee employee) {
        Contact contact = new Contact();
        contact.setEmployee(employee);
        contact.setName(employee.getFirstName() + " " + employee.getLastName());
        contact.setEmail(employee.getEmail());
        contact.setDepartment(employee.getDepartment());
        contact.setPhoneNumber(employee.getPhoneNumber());

        // Save the contact and send notification
        Contact savedContact = contactRepository.save(contact);

        // Send notification about the new contact creation
        String message = "New Contact Created: " + savedContact.getName() + " for employee " + employee.getFullName() + ".";
        notificationService.sendNotification(message, employee); // Notify employee

        // Notify admin (assuming you have a method to get the admin's email or ID)
        String adminMessage = "New Contact Created for Employee: " + employee.getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin

        return savedContact;
    }

    public Contact updateContactForEmployee(Employee employee) {
        Contact contact = contactRepository.findByEmployee(employee);
        if (contact == null) {
            throw new NoSuchElementException("Contact not found for employee: " + employee.getFullName());
        }

        contact.setName(employee.getFirstName() + " " + employee.getLastName());
        contact.setEmail(employee.getEmail());
        contact.setDepartment(employee.getDepartment());

        // Save the updated contact and send notification
        Contact updatedContact = contactRepository.save(contact);

        // Send notification about the contact update
        String message = "Contact Updated: " + updatedContact.getName() + " for employee " + employee.getFullName() + ".";
        notificationService.sendNotification(message, employee); // Notify employee

        // Notify admin
        String adminMessage = "Contact Updated for Employee: " + employee.getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin

        return updatedContact;
    }

    public void deactivateContactForEmployee(Employee employee) {
        Contact contact = contactRepository.findByEmployee(employee);
        if (contact == null) {
            throw new NoSuchElementException("Contact not found for employee: " + employee.getFullName());
        }

        contact.setActive(false);
        contactRepository.save(contact);

        // Send notification about the contact deactivation
        String message = "Contact Deactivated: " + contact.getName() + " for employee " + employee.getFullName() + " has been deactivated.";
        notificationService.sendNotification(message, employee); // Notify employee

        // Notify admin
        String adminMessage = "Contact Deactivated for Employee: " + employee.getFullName();
        notificationService.sendNotification(adminMessage, getAdminEmployee()); // Notify admin
    }

    public Optional<Contact> getContactByEmployee(Employee employee) {
        return Optional.ofNullable(contactRepository.findByEmployee(employee));
    }

    public List<Contact> getAllContacts() {
        return contactRepository.findAll(); // Assuming you have a method in your repository to find all contacts
    }

    // Example method to get the admin employee (you need to implement this based on your application logic)
    private Employee getAdminEmployee() {
        return employeeRepository.findByRole("ADMIN")
                .orElseThrow(() -> new NoSuchElementException("Admin employee not found"));
    }

    public Contact createContact(Contact contact) {
        // Logic to save the contact to the database
        return contactRepository.save(contact);
    }
}