package com.caleb.ERP.service;

import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.ContactRepository;
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
    private NotificationService notificationService; // Inject NotificationService

    public Contact createContactForEmployee(Employee employee) {
        // Check if a contact already exists for this employee
        Optional<Contact> existingContactOpt = getContactByEmployee(employee);
        if (existingContactOpt.isPresent()) {
            throw new IllegalArgumentException("Contact already exists for this employee.");
        }

        Contact contact = new Contact();
        contact.setEmployee(employee);
        contact.setName(employee.getFirstName() + " " + employee.getLastName());
        contact.setEmail(employee.getEmail());
        contact.setDepartment(employee.getDepartment());
        contact.setPhoneNumber(employee.getPhoneNumber());

        // Save the contact
        Contact savedContact = contactRepository.save(contact);

        // Send notification to admins
        String adminMessage = "A new contact has been created for employee: " + savedContact.getName();
        notificationService.sendAdminNotification(adminMessage);

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

        // Save the updated contact
        Contact updatedContact = contactRepository.save(contact);

        // Send notification to admins
        String adminMessage = "Contact details updated for employee: " + updatedContact.getName();
        notificationService.sendAdminNotification(adminMessage);

        return updatedContact;
    }

    public void deactivateContactForEmployee(Employee employee) {
        Contact contact = contactRepository.findByEmployee(employee);
        if (contact == null) {
            throw new NoSuchElementException("Contact not found for employee: " + employee.getFullName());
        }

        contact.setActive(false);
        contactRepository.save(contact);

        // Send notification to admins
        String adminMessage = "Contact has been deactivated for employee: " + contact.getName();
        notificationService.sendAdminNotification(adminMessage);
    }

    public Optional<Contact> getContactByEmployee(Employee employee) {
        return Optional.ofNullable(contactRepository.findByEmployee(employee));
    }

    public List<Contact> getAllContacts() {
        return contactRepository.findAll(); // Assuming you have a method in your repository to find all contacts
    }

    public Contact createContact(Contact contact) {
        // Check if a contact already exists for this employee
        Optional<Contact> existingContactOpt = getContactByEmployee(contact.getEmployee());
        if (existingContactOpt.isPresent()) {
            throw new IllegalArgumentException("Contact already exists for this employee.");
        }
        // Logic to save the contact to the database
        return contactRepository.save(contact);
    }
}