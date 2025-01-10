package com.caleb.ERP.service;

import com.caleb.ERP.entity.Contact;
import com.caleb.ERP.entity.Employee;
import com.caleb.ERP.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {
    @Autowired
    private ContactRepository contactRepository;

    public Contact createContactForEmployee(Employee employee) {
        Contact contact = new Contact();
        contact.setEmployee(employee);
        contact.setName(employee.getFirstName() + " " + employee.getLastName());
        contact.setEmail(employee.getEmail());
        contact.setDepartment(employee.getDepartment());
        contact.setPhoneNumber(employee.getPhoneNumber());
        return contactRepository.save(contact);
    }

    public Contact updateContactForEmployee(Employee employee) {
        Contact contact = contactRepository.findByEmployee(employee);
        if (contact != null) {
            contact.setName(employee.getFirstName() + " " + employee.getLastName());
            contact.setEmail(employee.getEmail());
            contact.setDepartment(employee.getDepartment());
            return contactRepository.save(contact);
        }
        return null; // or throw an exception
    }

    public void deactivateContactForEmployee(Employee employee) {
        Contact contact = contactRepository.findByEmployee(employee);
        if (contact != null) {
            contact.setActive(false);
            contactRepository.save(contact);
        }
    }

    public Optional<Contact> getContactByEmployee(Employee employee) {
        return Optional.ofNullable(contactRepository.findByEmployee(employee));
    }

    public List<Contact> getAllContacts() {
        return contactRepository.findAll(); // Assuming you have a method in your repository to find all contacts
    }
}