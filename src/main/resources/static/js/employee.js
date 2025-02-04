// Base URL for API requests
const BASE_URL = 'http://192.168.100.39:8082'; // Adjust this to your actual backend URL

// Function to fetch and display all overview counts
async function fetchOverviewCounts() {
    await fetchPendingLeavesCount();
    await fetchPendingFinancesCount();
    await fetchPendingTasksCount();
}
// Fetch count of pending leaves
async function fetchPendingLeavesCount() {
    try {
        const response = await fetch(`${BASE_URL}/api/leaves/me/pending`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch pending leaves');
        }

        const pendingLeaves = await response.json();
        console.log('Pending Leaves:', pendingLeaves); // Log the response
        document.getElementById('pendingLeavesCount').textContent = pendingLeaves.length; // Update count
    } catch (error) {
        console.error('Error fetching pending leaves count:', error);
        document.getElementById('pendingLeavesCount').textContent = '0'; // Set to 0 on error
    }
}

// Fetch count of pending finances
async function fetchPendingFinancesCount() {
    try {
        const response = await fetch(`${BASE_URL}/api/finances/me/pending`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch pending finances');
        }

        const pendingFinances = await response.json();
        document.getElementById('pendingFinancesCount').textContent = pendingFinances.length; // Update count
    } catch (error) {
        console.error('Error fetching pending finances count:', error);
        document.getElementById('pendingFinancesCount').textContent = '0'; // Set to 0 on error
    }
}

// Fetch count of pending tasks
async function fetchPendingTasksCount() {
    try {
        const response = await fetch(`${BASE_URL}/api/tasks/me/non-started`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch pending tasks');
        }

        const pendingTasks = await response.json();
        document.getElementById('pendingTasksCount').textContent = pendingTasks.length; // Update count
    } catch (error) {
        console.error('Error fetching pending tasks count:', error);
        document.getElementById('pendingTasksCount').textContent = '0'; // Set to 0 on error
    }
}

// Function to show the modal
function showEditLeaveRequestModal() {
    const modal = document.getElementById('editLeaveRequestModal');
    modal.setAttribute('aria-hidden', 'false'); // Set aria-hidden to false when showing the modal
    const editLeaveRequestModal = new bootstrap.Modal(modal);
    editLeaveRequestModal.show();
}

document.addEventListener('DOMContentLoaded', async function() {
    console.log('Employee dashboard script loaded');
    try {
        await loadPersonalInfo();
        console.log('Personal info loaded');
        await loadContacts();
        console.log('Contacts loaded');
        await loadLeaveBalances();
        console.log('Leave balances loaded');
        await loadLeaveHistory();
        console.log('Leave history loaded');
        await fetchFinanceRecords();
        console.log('Finance records fetched');
        await fetchTasks();
        console.log('Tasks loaded');
        await connectWebSocket(); // Connect to WebSocket for notifications
        console.log('Notifications fetched');
        await fetchOverviewCounts();

        showSection('overview'); // Show Overview by default
    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
    // Dark Mode Toggle
    document.getElementById('toggleDarkMode').addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        document.querySelector('.sidebar').classList.toggle('dark-mode');
        document.querySelectorAll('.card').forEach(card => card.classList.toggle('dark-mode'));
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

// Logout function
function logout() {
    console.log('Logout function called');
    localStorage.removeItem('userToken');
    window.location.href = '/'; // Redirect to the home page or login page
}

// Ensure the logout button in the sidebar is bound correctly
const sidebarLogoutButton = document.getElementById('sidebarLogoutButton');
if (sidebarLogoutButton) {
    sidebarLogoutButton.addEventListener('click', logout);
} else {
    console.error('Sidebar logout button not found in the DOM');
}

// Ensure the logout button in the top bar is bound correctly
const topBarLogoutButton = document.getElementById('topBarLogoutButton');
if (topBarLogoutButton) {
    topBarLogoutButton.addEventListener('click', logout);
} else {
    console.error('Top bar logout button not found in the DOM');
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

// Function to display contacts
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
};

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
    const defaultAllocatedDays = 21;

    // Function to determine the color based on usage percentage
    const getProgressBarColor = (used, total) => {
        const percentageUsed = (used / total) * 100;
        if (percentageUsed <= 50) {
            return 'bg-success'; // Green
        } else if (percentageUsed <= 80) {
            return 'bg-warning'; // Yellow
        } else {
            return 'bg-danger'; // Red
        }
    };

    // Update Sick Leave
    const sickLeaveUsed = defaultAllocatedDays - leaveBalances["Sick Leave Balance"];
    const sickLeaveRemaining = leaveBalances["Sick Leave Balance"];
    const sickLeaveProgress = (sickLeaveUsed / defaultAllocatedDays) * 100;

    document.getElementById('sickLeaveAllocated').textContent = defaultAllocatedDays;
    document.getElementById('sickLeaveRemaining').textContent = sickLeaveRemaining;
    const sickLeaveColor = getProgressBarColor(sickLeaveUsed, defaultAllocatedDays);
    const sickLeaveProgressBar = document.getElementById('sickLeaveProgress');
    sickLeaveProgressBar.style.width = `${sickLeaveProgress}%`;
    sickLeaveProgressBar.setAttribute('aria-valuenow', sickLeaveProgress);
    sickLeaveProgressBar.className = `progress-bar ${sickLeaveColor}`; // Set color class

    // Update Vacation Leave
    const vacationLeaveUsed = defaultAllocatedDays - leaveBalances["Vacation Leave Balance"];
    const vacationLeaveRemaining = leaveBalances["Vacation Leave Balance"];
    const vacationLeaveProgress = (vacationLeaveUsed / defaultAllocatedDays) * 100;

    document.getElementById('vacationLeaveAllocated').textContent = defaultAllocatedDays;
    document.getElementById('vacationLeaveRemaining').textContent = vacationLeaveRemaining;
    const vacationLeaveColor = getProgressBarColor(vacationLeaveUsed, defaultAllocatedDays);
    const vacationLeaveProgressBar = document.getElementById('vacationLeaveProgress');
    vacationLeaveProgressBar.style.width = `${vacationLeaveProgress}%`;
    vacationLeaveProgressBar.setAttribute('aria-valuenow', vacationLeaveProgress);
    vacationLeaveProgressBar.className = `progress-bar ${vacationLeaveColor}`; // Set color class

    // Update Paternity Leave
    const paternityLeaveUsed = defaultAllocatedDays - leaveBalances["Paternity Leave Balance"];
    const paternityLeaveRemaining = leaveBalances["Paternity Leave Balance"];
    const paternityLeaveProgress = (paternityLeaveUsed / defaultAllocatedDays) * 100;

    document.getElementById('paternityLeaveAllocated').textContent = defaultAllocatedDays; // This should be the allocated days
    document.getElementById('paternityLeaveRemaining').textContent = paternityLeaveRemaining;
    const paternityLeaveColor = getProgressBarColor(paternityLeaveUsed, defaultAllocatedDays);
    const paternityLeaveProgressBar = document.getElementById('paternityLeaveProgress');
    paternityLeaveProgressBar.style.width = `${paternityLeaveProgress}%`;
    paternityLeaveProgressBar.setAttribute('aria-valuenow', paternityLeaveProgress);
    paternityLeaveProgressBar.className = `progress-bar ${paternityLeaveColor}`; // Set color class

    // Update Compassionate Leave
    const compassionateLeaveUsed = defaultAllocatedDays - leaveBalances["Compassionate Leave Balance"];
    const compassionateLeaveRemaining = leaveBalances["Compassionate Leave Balance"];
    const compassionateLeaveProgress = (compassionateLeaveUsed / defaultAllocatedDays) * 100;

    document.getElementById('compassionateLeaveAllocated').textContent = defaultAllocatedDays; // This should be the allocated days
    document.getElementById('compassionateLeaveRemaining').textContent = compassionateLeaveRemaining;
    const compassionateLeaveColor = getProgressBarColor(compassionateLeaveUsed, defaultAllocatedDays);
    const compassionateLeaveProgressBar = document.getElementById('compassionateLeaveProgress');
    compassionateLeaveProgressBar.style.width = `${compassionateLeaveProgress}%`;
    compassionateLeaveProgressBar.setAttribute('aria-valuenow', compassionateLeaveProgress);
    compassionateLeaveProgressBar.className = `progress-bar ${compassionateLeaveColor}`; // Set color class
};

// Call loadLeaveBalances on page load to display current leave balances
document.addEventListener('DOMContentLoaded', loadLeaveBalances);
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

// Set today's date as the minimum date for leave request
document.addEventListener('DOMContentLoaded', () => {
    const today = new Date().toISOString().split('T')[0]; // Get today's date in YYYY-MM-DD format
    document.getElementById('startDate').setAttribute('min', today);
    document.getElementById('endDate').setAttribute('min', today);
});

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

    // Calculate the number of days taken
    const daysTaken = Math.ceil((new Date(leaveRequest.endDate) - new Date(leaveRequest.startDate)) / (1000 * 3600 * 24)) + 1; // Include end date
    const allocatedDays = 21; // This should be fetched from the server based on leave type
    if (daysTaken > allocatedDays) {
        document.getElementById('startDateError').textContent = `You cannot exceed the allocated ${allocatedDays} days.`;
        return; // Stop submission if days exceed allocated
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

    // Calculate the number of days taken
    const daysTaken = Math.ceil((new Date(updatedLeaveRequest.endDate) - new Date(updatedLeaveRequest.startDate)) / (1000 * 3600 * 24)) + 1; // Include end date
    const allocatedDays = 21; // This should be fetched from the server based on leave type
    if (daysTaken > allocatedDays) {
        document.getElementById('startDateError').textContent = `You cannot exceed the allocated ${allocatedDays} days.`;
        return; // Stop submission if days exceed allocated
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

        // Close the modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('editLeaveRequestModal'));
        modal.hide(); // Hide the modal

        // Refresh the leave history and balances
        await loadLeaveHistory(); // Refresh the leave history
        await loadLeaveBalances(); // Refresh leave balances to reflect any changes
    } catch (error) {
        console.error("Error updating leave request:", error);
        alert("An error occurred while updating the leave request. Please try again.");
    }
});


// Task Management Functions

let currentTaskId; // Variable to hold the current task ID

// Fetch all tasks and update the UI
async function fetchTasks() {
    const taskTableBody = document.getElementById('taskTableBody');
    taskTableBody.innerHTML = '<tr><td colspan="8" class="text-center"><div class="spinner-border" role="status"></div></td></tr>';

    try {
        const response = await fetch(`${BASE_URL}/api/tasks`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const tasks = await response.json();
        populateTaskList(tasks);
        populateTaskHistory(tasks);
    } catch (error) {
        console.error('Error fetching tasks:', error);
        taskTableBody.innerHTML = '<tr><td colspan="8" class="text-center text-danger">Failed to load tasks.</td></tr>';
    }
}

// Populate task list with active tasks
function populateTaskList(tasks) {
    const taskTableBody = document.getElementById('taskTableBody');
    taskTableBody.innerHTML = ''; // Clear existing rows

    tasks.forEach(task => {
        if (task.status !== 'completed') { // Only show active tasks
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
                    <button class="btn btn-info btn-sm" onclick="viewTaskDetails('${task.taskId}')">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            `;
            taskTableBody.appendChild(row);
        }
    });
}
// Populate task history with completed tasks
function populateTaskHistory(tasks) {
    const taskHistoryBody = document.getElementById('taskHistoryBody');
    taskHistoryBody.innerHTML = ''; // Clear existing rows

    tasks.forEach(task => {
        if (task.status === 'completed') { // Only show completed tasks
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${task.taskName}</td>
                <td>${task.assignedToName}</td>
                <td>${task.assignedByName}</td>
                <td>${task.description}</td>
                <td>${task.dueDate}</td>
                <td><span class="badge bg-success">Completed</span></td>
                <td><input type="checkbox" disabled ${task.urgent ? 'checked' : ''}></td>
                <td>${new Date().toLocaleDateString()}</td>
                <td>
                    <button class="btn btn-info btn-sm" onclick="viewTaskDetails('${task.taskId}')">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            `;
            taskHistoryBody.appendChild(row);
        }
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
// Set up date validation for requisition and claim forms
document.addEventListener('DOMContentLoaded', function() {
    const today = new Date().toISOString().split('T')[0]; // Get today's date in YYYY-MM-DD format

    // Set min date for requisition (today or later)
    document.getElementById('dateSubmitted').setAttribute('min', today);

    // Set max date for claim (today or earlier)
    document.getElementById('claimDateSubmitted').setAttribute('max', today);
});

// Function to handle the submission of the requisition form
document.getElementById('requisition-form').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form

    try {
        const response = await fetch('/api/finances/requisition', {
            method: 'POST',
            body: formData,
            headers: getAuthHeaders(true) // Pass true for isMultipart
        });

        if (!response.ok) {
            const errorData = await response.json();
            displayError('requisitionDateError', errorData.error || 'Failed to submit requisition');
            return;
        }

        alert('Requisition submitted successfully!');
        this.reset(); // Reset the form
        $('#requisitionModal').modal('hide'); // Close the requisition modal
        fetchFinanceRecords(); // Refresh the finance records table
        fetchFinanceHistory(); // Refresh the finance history table
    } catch (error) {
        console.error('Error:', error);
        displayError('requisitionDateError', 'Error submitting requisition: ' + error.message);
    }
});

// Function to handle the submission of the claim form
document.getElementById('claim-form').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form

    try {
        const response = await fetch('/api/finances/claim', {
            method: 'POST',
            body: formData,
            headers: getAuthHeaders(true) // Pass true for isMultipart
        });

        if (!response.ok) {
            const errorData = await response.json();
            displayError('claimDateError', errorData.error || 'Failed to submit claim');
            return;
        }

        alert('Claim submitted successfully!');
        this.reset(); // Reset the form
        $('#claimModal').modal('hide'); // Close the claim modal
        fetchFinanceRecords(); // Refresh the finance records table
        fetchFinanceHistory(); // Refresh the finance history table
    } catch (error) {
        console.error('Error:', error);
        displayError('claimDateError', 'Error submitting claim: ' + error.message);
    }
});

// Function to display error messages
function displayError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    errorElement.textContent = message; // Set the error message
}

// Function to fetch and display finance records
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
                <td>${parseFloat(finance.amount).toFixed(2)} KES</td>
                <td>${new Date(finance.dateSubmitted).toLocaleDateString()}</td> <!-- Display the date -->
                <td>
                    <span class="status-badge ${finance.status.toLowerCase()}">
                        ${finance.status}
                    </span>
                </td>
                <td>
                    ${finance.type === 'Claim' ? `<button onclick="downloadFinanceFile('${finance.financeId}')" class="download-button">Download</button>` : ''}
                </td>
            `;
            recordsBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching finance records: ' + error.message);
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

// NOTIFICATIONS
let stompClient = null;
let notificationCount = 0;
let notifications = []; // Store notifications in an array

function connectWebSocket() {
    const token = localStorage.getItem('jwt'); // Retrieve the JWT from local storage
    const socket = new SockJS(`http://192.168.100.39:8082/notifications?token=${token}`); // Include token in the URL
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        // Subscribe to the employee notifications topic
        stompClient.subscribe('/topic/employees', function (notification) {
            handleNotification(JSON.parse(notification.body));
        });
    }, function (error) {
        console.error('WebSocket connection error:', error);
    });
}

// Function to handle incoming notifications
function handleNotification(notification) {
    console.log('New notification:', notification); // Debugging log

    // Check if notification has the expected structure
    if (!notification.message) {
        console.error('Notification does not have a message property:', notification);
        return;
    }

    // Add the notification to the notifications array with a read status
    notifications.push({ ...notification, read: false });

    // Display the notification
    displayNotifications();
    updateNotificationCount(); // Update the notification count
}

// Function to display notifications
function displayNotifications() {
    const notificationDisplayArea = document.getElementById('notificationDisplayArea');
    notificationDisplayArea.innerHTML = ''; // Clear existing notifications

    notifications.forEach((notification, index) => {
        const notificationItem = document.createElement('div');
        notificationItem.className = `alert alert-info ${notification.read ? 'read' : ''}`; // Add a class if read
        notificationItem.innerText = notification.message; // Adjust based on your notification structure

        // Add a button to mark this notification as read
        const markAsReadButton = document.createElement('button');
        markAsReadButton.className = 'btn btn-success btn-sm float-right';
        markAsReadButton.innerText = 'Mark as Read';
        markAsReadButton.onclick = function() {
            markNotificationAsRead(index); // Mark this notification as read
        };

        notificationItem.appendChild(markAsReadButton); // Append the button
        notificationDisplayArea.appendChild(notificationItem); // Append the notification item
    });
}

// Function to update the notification count
function updateNotificationCount() {
    const notificationCountElement = document.getElementById('notificationCount');
    notificationCountElement.innerText = notifications.filter(n => !n.read).length; // Count unread notifications
}

// Function to mark a specific notification as read
function markNotificationAsRead(index) {
    notifications[index].read = true; // Set the read status to true
    displayNotifications(); // Refresh the display
}

// Mark all notifications as read
document.getElementById('markAllAsRead').addEventListener('click', function () {
    notifications.forEach(notification => notification.read = true); // Mark all as read
    displayNotifications(); // Refresh the display
});

// Link the notification button to the notification section
document.getElementById('notificationButton').addEventListener('click', function() {
    showSection('notificationManagement'); // Show the notification section
});

