// Base URL for API requests
const BASE_URL = 'http://localhost:8080'; // Adjust this to your actual backend URL
// Function to show the modal
function showEditLeaveRequestModal() {
    const modal = document.getElementById('editLeaveRequestModal');
    modal.setAttribute('aria-hidden', 'false'); // Set aria-hidden to false when showing the modal
    const editLeaveRequestModal = new bootstrap.Modal(modal);
    editLeaveRequestModal.show();
}

// Function to hide the modal
function hideEditLeaveRequestModal() {
    const modal = document.getElementById('editLeaveRequestModal');
    modal.setAttribute('aria-hidden', 'true'); // Set aria-hidden to true when hiding the modal
    const editLeaveRequestModal = bootstrap.Modal.getInstance(modal);
    if (editLeaveRequestModal) {
        editLeaveRequestModal.hide();
    }
}

// Load initial data
document.addEventListener('DOMContentLoaded', function() {
    console.log('Employee dashboard script loaded');
    loadPersonalInfo(); // Load personal information when the dashboard is loaded
    loadContacts(); // Load contacts for the Contact Directory module
    loadLeaveBalances();
    loadLeaveHistory(); // Load leave history
    fetchFinanceRecords(); // Fetch finance records on page load
    fetchFinanceHistory();
    fetchTasks(); // Load tasks when the dashboard is loaded

    // Ensure the search input exists before adding an event listener
    const searchInput = document.getElementById('searchContacts');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const contactCards = document.querySelectorAll('.card'); // Select all contact cards

            contactCards.forEach(card => {
                const contactName = card.querySelector('.card-title').textContent.toLowerCase();
                const department = card.querySelector('.card-text:nth-child(2)').textContent.toLowerCase();
                const phone = card.querySelector('.card-text:nth-child(3)').textContent.toLowerCase();

                // Show or hide the card based on the search term
                if (contactName.includes(searchTerm) || department.includes(searchTerm) || phone.includes(searchTerm)) {
                    card.parentElement.style.display = ''; // Show the card
                } else {
                    card.parentElement.style.display = 'none'; // Hide the card
                }
            });
        });
    }

    // Dark Mode Toggle
    document.getElementById('toggleDarkMode').addEventListener('click', function() {
        document.body.classList.toggle('dark-mode');
        if (document.body.classList.contains('dark-mode')) {
            localStorage.setItem('darkMode', 'enabled');
        } else {
            localStorage.removeItem('darkMode');
        }
    });

    // Check for saved user preference on page load
    if (localStorage.getItem('darkMode') === 'enabled') {
        document.body.classList.add('dark-mode');
    }

    // Event listener for the edit profile button
    document.getElementById('editProfileButton').addEventListener('click', async function() {
        try {
            const employee = await fetchEmployeeData(); // Fetch the employee data
            populateEditModal(employee); // Populate the modal with the fetched data
            $('#editProfileModal').modal('show'); // Show the modal
        } catch (error) {
            console.error('Error fetching employee data:', error);
        }
    });

    // Add event listeners for sidebar links
    const sidebarLinks = document.querySelectorAll('.sidebar a');
    sidebarLinks.forEach(link => {
        link.addEventListener('click', function(event) {
            event.preventDefault(); // Prevent default anchor behavior
            const sectionId = this.getAttribute('onclick').match(/'([^']+)'/)[1]; // Extract section ID from onclick
            showSection(sectionId); // Call showSection with the extracted ID
        });
    });
});

// Function to show the selected section
function showSection(sectionId) {
    const sections = document.querySelectorAll('.dashboard-section');
    sections.forEach(section => {
        if (section.id === sectionId) {
            section.classList.remove('hidden');
        } else {
            section.classList.add('hidden');
        }
    });
}

// Logout function
function logout() {
    console.log('Logout function called');
    localStorage.removeItem('userToken');
    window.location.href = '/';
}

// Ensure the logout button is bound correctly
const logoutButton = document.getElementById('logoutButton');
if (logoutButton) {
    logoutButton.addEventListener('click', logout);
} else {
    console.error('Logout button not found in the DOM');
}

// Utility function to get authentication headers
function getAuthHeaders(isMultipart = false) {
    const token = localStorage.getItem('jwt');
    if (!token) {
        console.error('No JWT found in local storage.');
        return {};
    }

    const decodedToken = jwt_decode(token);
    const employeeId = decodedToken.employeeId;

    // For multipart/form-data requests, don't set Content-Type
    if (isMultipart) {
        return {
            'Authorization': `Bearer ${token}`,
            'X-Employee-Id': employeeId
        };
    }

    // For regular JSON requests, include Content-Type
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-Employee-Id': employeeId
    };
}
// Function to load personal information
async function loadPersonalInfo() {
    const response = await fetch(`${BASE_URL}/api/employees/me`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (response.ok) {
        const employee = await response.json();
        document.getElementById('firstName').textContent = employee.firstName;
        document.getElementById('lastName').textContent = employee.lastName;
        document.getElementById('email').textContent = employee.email;
        document.getElementById('phone').textContent = employee.phoneNumber;
        document.getElementById('department').textContent = employee.department;
        document.getElementById('dateOfJoining').textContent = employee.dateOfJoining;
        document.getElementById('emergencyContactName').textContent = employee.emergencyContactName;
        document.getElementById('emergencyContactPhone').textContent = employee.emergencyContactPhone;

        // Load contacts after personal info is loaded
        loadContacts();

        // Fetch and display the profile picture
        const profilePictureElement = document.getElementById('profilePicture');
        const pictureResponse = await fetch(`${BASE_URL}/api/employees/${employee.employeeId}/profile-picture`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwt')}` // Include JWT token
            }
        });

        if (pictureResponse.ok) {
            const imageBlob = await pictureResponse.blob();
            const imageUrl = URL.createObjectURL(imageBlob);
            profilePictureElement.src = imageUrl;
        } else {
            console.error('Failed to load profile picture:', pictureResponse.status);
            profilePictureElement.src = 'uploads/default-profile.png';
        }
    } else {
        console.error('Failed to load personal information:', response.status);
    }
}

const displayContacts = (contacts) => {
    const contactsContainer = document.getElementById('contactsContainer'); // Get the container
    contactsContainer.innerHTML = ''; // Clear previous contacts

    if (!contacts || contacts.length === 0) {
        console.log('No contacts to display');
        return;
    }

    contacts.forEach(contact => {
        // Create a new card for each contact
        const card = document.createElement('div');
        card.classList.add('contact-card'); // Use the new class for flexbox layout

        // Define the structure for contact information
        card.innerHTML = `
            <h5 class="card-title">${contact.name}</h5> <!-- Use the name property -->
            <p class="card-text"><strong>Department:</strong> <span>${contact.department}</span></p>
            <p class="card-text"><strong>Phone:</strong> <span>${contact.phoneNumber}</span></p>
            <p class="card-text"><strong>Email:</strong> <span>${contact.email}</span></p>
            <span class="badge ${contact.active ? 'bg-success' : 'bg-danger'}">${contact.active ? 'Active' : 'Inactive'}</span>
        `;

        // Append the card to the contacts container
        contactsContainer.appendChild(card);
    });
};
// Load contacts function
async function loadContacts() {
    try {
        const response = await fetch(`${BASE_URL}/api/contacts`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const contacts = await response.json();

            // Check if contacts is an array and has at least one object
            if (Array.isArray(contacts) && contacts.length > 0) {
                displayContacts(contacts); // Pass the entire array to displayContacts
            } else {
                console.error('No contacts found or data is not in expected format.');
            }

            document.getElementById('contactDirectory').classList.remove('hidden'); // Show the contact directory
        } else {
            console.error('Failed to load contacts:', response.status);
        }
    } catch (error) {
        console.error("Error fetching contacts:", error);
    }
}
// Handle profile picture upload
document.getElementById('profilePictureUpload').addEventListener('change', function(event) {
    const file = event.target.files[0]; // Get the selected file
    if (file) {
        const formData = new FormData();
        formData.append('file', file); // Ensure the key matches what the server expects

        // Use the employee ID from the decoded JWT
        const employeeId = jwt_decode(localStorage.getItem('jwt')).employeeId;

        // Display the uploaded image immediately
        const profilePictureElement = document.getElementById('profilePicture');
        const reader = new FileReader();

        // Set the src of the profile picture to the uploaded image
        reader.onload = function(e) {
            profilePictureElement.src = e.target.result; // Set the src to the uploaded image
        };

        reader.readAsDataURL(file); // Read the file as a data URL

        // Upload the profile picture to the server
        fetch(`${BASE_URL}/api/employees/${employeeId}/upload-profile-picture`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwt')}`, // Include your JWT token here
            },
            body: formData, // Do not set Content-Type here; let the browser do it
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to upload profile picture');
                }
                return response.text(); // Get the response message
            })
            .then(message => {
                alert(message); // Notify the user of the upload success
                loadPersonalInfo(); // Refresh the personal information displayed
            })
            .catch(error => console.error('Error uploading profile picture:', error));
    }
});

// Function to fetch employee data
async function fetchEmployeeData() {
    const response = await fetch(`${BASE_URL}/api/employees/me`, {
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        throw new Error('Failed to fetch employee data');
    }
    return response.json();
}

// Function to populate the edit modal
function populateEditModal(employee) {
    document.getElementById('editFirstName').value = employee.firstName;
    document.getElementById('editLastName').value = employee.lastName;
    document.getElementById('editPhone').value = employee.phoneNumber;
    document.getElementById('editDepartment').value = employee.department;
    document.getElementById('editEmergencyContactName').value = employee.emergencyContactName;
    document.getElementById('editEmergencyContactPhone').value = employee.emergencyContactPhone;
}

// Event listener for the Edit Profile button
document.getElementById('editProfileButton').addEventListener('click', async function() {
    try {
        const employee = await fetchEmployeeData(); // Fetch employee data
        populateEditModal(employee); // Populate the modal with employee data
        $('#editProfileModal').modal('show'); // Show the modal
    } catch (error) {
        console.error('Error fetching employee data:', error);
        alert('Error loading employee data: ' + error.message);
    }
});

// Handle profile update
document.getElementById('editProfileForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent the default form submission

    // Create an object to hold the updated employee data
    const updatedEmployee = {
        firstName: document.getElementById('editFirstName').value,
        lastName: document.getElementById('editLastName').value,
        phoneNumber: document.getElementById('editPhone').value,
        department: document.getElementById('editDepartment').value,
        emergencyContactName: document.getElementById('editEmergencyContactName').value,
        emergencyContactPhone: document.getElementById('editEmergencyContactPhone').value,
    };

    // Send a PUT request to update the employee data
    fetch(`${BASE_URL}/api/employees/me`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            ...getAuthHeaders() // Include authentication headers
        },
        body: JSON.stringify(updatedEmployee), // Convert the updated employee object to JSON
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to update employee data'); // Handle non-200 responses
            }
            return response.json(); // Parse the JSON response
        })
        .then(data => {
        alert('Profile updated successfully!'); // Notify the user of success
        loadPersonalInfo(); // Refresh the personal information displayed
        $('#editProfileModal').modal('hide'); // Hide the modal after successful update
    })
    .catch(error => {
        console.error('Error updating employee data:', error); // Log any errors
        alert('Error updating profile: ' + error.message); // Notify the user of the error
    });
});

// Handle change password
document.getElementById('changePasswordForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent the default form submission

    const newPassword = document.getElementById('newPassword').value;
    const currentPassword = document.getElementById('currentPassword').value;

    fetch(`${BASE_URL}/api/employees/me/change-password`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ currentPassword, newPassword })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Failed to change password: ${text}`); // Log the response text for debugging
            });
        }
        return response.text(); // Change this to text() if the server returns plain text
    })
    .then(message => {
        alert(message); // Display the message returned from the server
        $('#changePasswordModal').modal('hide'); // Hide the modal after successful update
    })
    .catch(error => {
        console.error('Error changing password:', error);
        alert('Error changing password: ' + error.message);
    });
});

// Leave Management Functions

// Function to load leave balances
async function loadLeaveBalances() {
    try {
        const response = await fetch(`${BASE_URL}/api/leaves/me/balance`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const leaveBalances = await response.json();
            displayLeaveBalances(leaveBalances);
        } else {
            console.error('Failed to load leave balances:', response.status);
        }
    } catch (error) {
        console.error("Error fetching leave balances:", error);
    }
}

const displayLeaveBalances = (leaveBalances) => {
    const leaveBalanceBody = document.getElementById('leaveBalanceBody');
    leaveBalanceBody.innerHTML = ''; // Clear previous balances

    for (const [leaveType, balance] of Object.entries(leaveBalances)) {
        const row = document.createElement('tr');
        const type = leaveType.split(' ')[0]; // Extracting the leave type (e.g., "Sick")
        row.innerHTML = `
            <td>${type} Leave</td>
            <td>21</td> <!-- Assuming 21 days allocated -->
            <td>${balance}</td>
        `;
        leaveBalanceBody.appendChild(row);
    }
};

// Function to load leave history
async function loadLeaveHistory() {
    try {
        const response = await fetch(`${BASE_URL}/api/leaves/me/history`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const leaveHistory = await response.json();
            displayLeaveHistory(leaveHistory);
        } else {
            console.error('Failed to load leave history:', response.status);
        }
    } catch (error) {
        console.error("Error fetching leave history:", error);
    }
}

const displayLeaveHistory = (leaveHistory) => {
    const leaveHistoryBody = document.getElementById('leaveHistoryBody');
    leaveHistoryBody.innerHTML = ''; // Clear previous history

    leaveHistory.forEach(leave => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${leave.leaveType}</td>
            <td>${leave.startDate}</td>
            <td>${leave.endDate}</td>
            <td>${leave.reason}</td>
            <td>${leave.approverName || 'N/A'}</td>
            <td>${leave.dateRequested}</td>
            <td>${leave.daysTaken}</td>
            <td><span class="badge ${leave.status === 'Approved' ? 'bg-success' : 'bg-warning'}">${leave.status}</span></td>
            <td>
                ${leave.status === 'Pending' ? `<button class="btn btn-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#editLeaveRequestModal" onclick="populateEditLeaveForm('${leave.leaveId}')">Update Leave</button>` : ''}
            </td>
        `;
        leaveHistoryBody.appendChild(row);
    });
};

// Validate leave request form
const validateLeaveRequest = (leaveRequest) => {
    const today = new Date();
    const startDate = new Date(leaveRequest.startDate);
    const endDate = new Date(leaveRequest.endDate);
    let isValid = true;

    // Clear previous error messages
    document.getElementById('leaveTypeError').textContent = '';
    document.getElementById('startDateError').textContent = '';
    document.getElementById('endDateError').textContent = '';
    document.getElementById('reasonError').textContent = '';

    if (!leaveRequest.leaveType) {
        document.getElementById('leaveTypeError').textContent = "Please select a leave type.";
        isValid = false;
    }
    if (startDate < today) {
        document.getElementById('startDateError').textContent = "Start date cannot be in the past.";
        isValid = false;
    }
    if (endDate < startDate) {
        document.getElementById('endDateError').textContent = "End date must be after the start date.";
        isValid = false;
    }
    if (!leaveRequest.reason) {
        document.getElementById('reasonError').textContent = "Please provide a reason for the leave.";
        isValid = false;
    }
    return isValid; // Return true if valid, false otherwise
};

// Handle leave request submission
document.getElementById('leaveRequestForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const leaveRequest = {
        leaveType: document.getElementById('leaveType').value,
        startDate: document.getElementById('startDate').value,
        endDate: document.getElementById('endDate').value,
        reason: document.getElementById('reason').value,
    };

    // Validate the leave request
    const isValid = validateLeaveRequest(leaveRequest);
    if (!isValid) {
        return; // Stop submission if validation fails
    }

    try {
        const response = await fetch(`${BASE_URL}/api/leaves`, {
            method: 'POST',
            headers: {
                ...getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(leaveRequest)
        });

        if (!response.ok) throw new Error('Failed to submit leave request');
        alert("Leave request submitted successfully!");
        loadLeaveHistory(); // Refresh the leave history
    } catch (error) {
        console.error("Error submitting leave request:", error);
    }
});

// Populate edit leave form
window.populateEditLeaveForm = (leaveId) => {
    fetch(`${BASE_URL}/api/leaves/${leaveId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Error fetching leave details");
            }
            return response.json();
        })
        .then(leave => {
            document.getElementById('editLeaveType').value = leave.leaveType;
            document.getElementById('editStartDate').value = leave.startDate;
            document.getElementById('editEndDate').value = leave.endDate;
            document.getElementById('editReason').value = leave.reason;
            document.getElementById('editLeaveId').value = leave.leaveId; // Store leave ID for updating

            // Show the modal after populating the fields
            showEditLeaveRequestModal();
        })
        .catch(error => {
            console.error("Error fetching leave details:", error);
            alert("Error fetching leave details: " + error.message);
        });
};

// Handle leave request update
document.getElementById('editLeaveRequestForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const leaveId = document.getElementById('editLeaveId').value;
    const updatedLeaveRequest = {
        leaveType: document.getElementById('editLeaveType').value,
        startDate: document.getElementById('editStartDate').value,
        endDate: document.getElementById('editEndDate').value,
        reason: document.getElementById('editReason').value,
    };

    // Validate the updated leave request
    const isValid = validateLeaveRequest(updatedLeaveRequest);
    if (!isValid) {
        return; // Stop submission if validation fails
    }

    try {
        const response = await fetch(`${BASE_URL}/api/leaves/${leaveId}`, {
            method: 'PUT',
            headers: {
                ...getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updatedLeaveRequest)
        });

        if (!response.ok) throw new Error('Failed to update leave request');
        alert("Leave request updated successfully!");
        loadLeaveHistory(); // Refresh the leave history
        hideEditLeaveRequestModal(); // Hide the modal after successful update
    } catch (error) {
        console.error("Error updating leave request:", error);
    }
});

// Task Management Functions

let currentTaskId; // Variable to hold the current task ID

// Function to fetch all tasks and update the UI
async function fetchTasks() {
    try {
        const response = await fetch(`${BASE_URL}/api/tasks`, {
            method: 'GET',
            headers: getAuthHeaders() // Use the utility function to get headers
        });

        // Check if the response is OK (status code 200-299)
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Parse the JSON response
        const tasks = await response.json();

        // Check if tasks is an array
        if (!Array.isArray(tasks)) {
            throw new TypeError('Expected tasks to be an array');
        }

        // Update the UI with the fetched tasks
        populateTaskList(tasks);
        populateTaskHistory(tasks);
    } catch (error) {
        console.error('Error fetching tasks:', error);
    }
}

// Function to populate task list in table view only
function populateTaskList(tasks) {
    const taskTableBody = document.getElementById('taskTableBody');
    taskTableBody.innerHTML = ''; // Clear existing table rows

    tasks.forEach(task => {
        const row = document.createElement('tr');
        row.innerHTML = `
                <td>${task.taskName}</td>
                <td>${task.assignedToName}</td>
                <td>${task.assignedByName}</td>
                <td>${task.description}</td>
                <td>${task.dueDate}</td>
                <td>
                    <select class="form-control" onchange="updateTaskStatus('${task.taskId}', this.value)">
                        <option value="not-started" ${task.status === 'not-started' ? 'selected' : ''}>Not Started</option>
                        <option value="in-progress" ${task.status === 'in-progress' ? 'selected' : ''}>In Progress</option>
                        <option value="completed" ${task.status === 'completed' ? 'selected' : ''}>Completed</option>
                    </select>
                </td>
                <td><input type="checkbox" disabled ${task.urgent ? 'checked' : ''}></td>
                <td>
                    <button class="btn btn-info" onclick="viewTaskDetails('${task.taskId}')">View</button>
                    <button class="btn btn-warning" onclick="openEditModal('${task.taskId}')">Edit</button>
                </td>
            `;
        taskTableBody.appendChild(row);
    });
}

// Function to populate task history
function populateTaskHistory(tasks) {
    const taskHistoryBody = document.getElementById('taskHistoryBody');
    taskHistoryBody.innerHTML = ''; // Clear existing history

    tasks.forEach(task => {
        if (task.status === 'completed') {
            const row = document.createElement('tr');
            row.innerHTML = `
                    <td>${task.taskName}</td>
                    <td>${task.assignedToName}</td>
                    <td>${task.assignedByName}</td>
                    <td>${task.description}</td>
                    <td>${task.dueDate}</td>
                    <td>${task.status}</td>
                    <td><input type="checkbox" disabled ${task.urgent ? 'checked' : ''}></td>
                    <td>${new Date().toLocaleDateString()}</td>
                `;
            taskHistoryBody.appendChild(row);
        }
    });
}

// Function to open the edit task modal
function openEditModal(taskId) {
    currentTaskId = taskId; // Set the current task ID
    getTaskById(taskId).then(task => {
        // Populate the modal fields with task details
        document.getElementById('taskName').value = task.taskName;
        document.getElementById('taskDescription').value = task.description;
        document.getElementById('dueDate').value = task.dueDate;
        document.getElementById('urgent').checked = task.urgent;
        document.getElementById('status').value = task.status;

        $('#editTaskModal').modal('show'); // Show the modal
    });
}

// Function to fetch a task by ID
async function getTaskById(taskId) {
    try {
        const response = await fetch(`${BASE_URL}/api/tasks/${taskId}`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch task: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching task:', error);
    }
}

// Function to submit the edited task
async function submitEditTask() {
    const taskData = {
        taskName: document.getElementById('taskName').value,
        description: document.getElementById('taskDescription').value,
        dueDate: document.getElementById('dueDate').value,
        urgent: document.getElementById('urgent').checked,
        status: document.getElementById('status').value
    };

    try {
        const response = await fetch(`${BASE_URL}/api/tasks/${currentTaskId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify(taskData)
        });

        if (!response.ok) {
            throw new Error(`Failed to update task: ${response.status}`);
        }

        // If the status is completed, move the task to history
        if (taskData.status === 'completed') {
            await moveTaskToHistory(currentTaskId);
        }

        $('#editTaskModal').modal('hide'); // Hide the modal
        fetchTasks(); // Refresh the task list
    } catch (error) {
        console.error('Error updating task:', error);
    }
}

// Function to move a task to history
async function moveTaskToHistory(taskId) {
    try {
        const response = await fetch(`${BASE_URL}/api/tasks/${taskId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify({ status: 'completed' })
        });

        if (!response.ok) {
            throw new Error(`Failed to move task to history: ${response.status}`);
        }

        fetchTasks(); // Refresh the task list
    } catch (error) {
        console.error('Error moving task to history:', error);
    }
}

// Function to update task status
async function updateTaskStatus(taskId, status) {
    try {
        const response = await fetch(`${BASE_URL}/api/tasks/${taskId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders() // Use the utility function to get headers
            },
            body: JSON.stringify({ status })
        });

        if (!response.ok) {
            throw new Error(`Failed to update task status: ${response.status}`);
        }

        fetchTasks(); // Refresh the task list
    } catch (error) {
        console.error('Error updating task status:', error);
    }
}

// Function to view task details
function viewTaskDetails(taskId) {
    getTaskById(taskId).then(task => {
        document.getElementById('viewTaskName').innerText = task.taskName;
        document.getElementById('viewAssignedTo').innerText = task.assignedToName;
        document.getElementById('viewAssignedBy').innerText = task.assignedByName;
        document.getElementById('viewDescription').innerText = task.description;
        document.getElementById('viewDueDate').innerText = task.dueDate;
        document.getElementById('viewStatus').innerText = task.status;
        document.getElementById('viewUrgent').innerText = task.urgent ? 'Yes' : 'No';

        $('#taskViewModal').modal('show'); // Show the modal
    });
}
// Finance Management
//Function to handle the submission of the requisition form
document.getElementById('requisition-form').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form
    const file = document.getElementById('file').files[0]; // Get the file input

    // Add file to FormData
    if (file) {
        formData.append('file', file);
    }

    try {
        const response = await fetch('/api/finances/requisition', {
            method: 'POST',
            body: formData,
            headers: getAuthHeaders(true) // Pass true for isMultipart
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to submit requisition');
        }

        alert('Requisition submitted successfully!');
        this.reset(); // Reset the form
        fetchFinanceRecords(); // Refresh the finance records table
        fetchFinanceHistory(); // Refresh the finance history table
    } catch (error) {
        console.error('Error:', error);
        alert('Error submitting requisition: ' + error.message);
    }
});

// Function to handle the submission of the claim form
document.getElementById('claim-form').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form
    const file = document.getElementById('claimFile').files[0]; // Get the file input

    // Add file to FormData
    if (file) {
        formData.append('file', file);
    }

    try {
        const response = await fetch('/api/finances/claim', {
            method: 'POST',
            body: formData,
            headers: getAuthHeaders(true) // Pass true for isMultipart
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to submit claim');
        }

        alert('Claim submitted successfully!');
        this.reset(); // Reset the form
        fetchFinanceRecords(); // Refresh the finance records table
        fetchFinanceHistory(); // Refresh the finance history table
    } catch (error) {
        console.error('Error:', error);
        alert('Error submitting claim: ' + error.message);
    }
});

// Fetch and display finance records
async function fetchFinanceRecords() {
    try {
        const response = await fetch('/api/finances/me', {
            headers: getAuthHeaders() // Use the getAuthHeaders function
        });

        if (!response.ok) {
            throw new Error('Failed to fetch finance records');
        }

        const finances = await response.json();
        const recordsBody = document.getElementById('finance-records-body');
        recordsBody.innerHTML = ''; // Clear existing rows

        finances.forEach(finance => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${finance.type}</td>
                <td>${finance.purpose || finance.expenseType || '-'}</td>
                <td>$${parseFloat(finance.amount).toFixed(2)}</td>
                <td>${new Date(finance.dateSubmitted).toLocaleDateString()}</td> <!-- Display the date -->
                <td>
                    <span class="status-badge ${finance.status.toLowerCase()}">
                        ${finance.status}
                    </span>
                </td>
                <td>
                    <button onclick="downloadFinanceFile('${finance.financeId}')" class="download-button">
                        Download
                    </button>
                </td>
            `;
            recordsBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching finance records: ' + error.message);
    }
}

// Fetch and display finance history
async function fetchFinanceHistory() {
    try {
        const response = await fetch('/api/finances/me/history', {
            headers: getAuthHeaders() // Use the getAuthHeaders function
        });

        if (!response.ok) {
            throw new Error('Failed to fetch finance history');
        }

        const history = await response.json();
        const historyBody = document.getElementById('finance-history-body');
        historyBody.innerHTML = ''; // Clear previous history

        history.forEach(finance => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${finance.type}</td>
                <td>${finance.purpose || finance.expenseType || '-'}</td>
                <td>$${parseFloat(finance.amount).toFixed(2)}</td>
                <td>${new Date(finance.dateSubmitted).toLocaleDateString()}</td>
                <td>
                    <span class="status-badge ${finance.status.toLowerCase()}">
                        ${finance.status}
                    </span>
                </td>
                <td>
                    <button onclick="downloadFinanceFile('${finance.financeId}')" class="download-button">
                        Download
                    </button>
                </td>
            `;
            historyBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching finance history: ' + error.message);
    }
}
// Function to download the supporting document associated with a finance record
function downloadFinanceFile(financeId) {
    fetch(`/api/finances/${financeId}/download`, {
        method: 'GET',
        headers: getAuthHeaders() // Use the getAuthHeaders function
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to download file');
            }
            return response.blob(); // Get the file as a Blob
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `finance_document_${financeId}`; // Customize the filename
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url); // Clean up the URL object
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error downloading file: ' + error.message);
        });
}