// Utility function to get authentication headers
function getAuthHeaders() {
    const token = localStorage.getItem('jwt'); // Retrieve the JWT from local storage
    if (!token) {
        console.error('No JWT found in local storage.');
        return {};
    }

    console.log('JWT:', token); // Log the JWT to see if it's retrieved correctly

    try {
        const decodedToken = jwt_decode(token); // Decode JWT to extract payload
        console.log('Decoded Token:', decodedToken); // Log the decoded token

        const employeeId = decodedToken.employeeId; // Store employeeId globally
        if (!employeeId) {
            console.error('Employee ID is not found in the decoded token.');
            return {};
        }

        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`, // Include the JWT in the Authorization header
            'X-Employee-Id': employeeId, // Include employeeId as a custom header
            employeeId // Return employeeId for use in other functions
        };
    } catch (error) {
        console.error('Error decoding JWT:', error);
        return {};
    }
}

const BASE_URL = 'http://localhost:8082/api'; // Adjust the base URL as needed

async function fetchOverviewData() {
    try {
        const [employeesResponse, leavesResponse, tasksResponse, financesResponse, departmentStatsResponse] = await Promise.all([
            fetch(`${BASE_URL}/employees`, { headers: getAuthHeaders() }),
            fetch(`${BASE_URL}/leaves`, { headers: getAuthHeaders() }),
            fetch(`${BASE_URL}/tasks`, { headers: getAuthHeaders() }),
            fetch(`${BASE_URL}/finances`, { headers: getAuthHeaders() }),
            fetch(`${BASE_URL}/employees/department-stats`, { headers: getAuthHeaders() }) // New endpoint for department stats
        ]);

        if (!employeesResponse.ok) {
            throw new Error('Failed to fetch employees');
        }
        if (!leavesResponse.ok) {
            throw new Error('Failed to fetch leaves');
        }
        if (!tasksResponse.ok) {
            throw new Error('Failed to fetch tasks');
        }
        if (!financesResponse.ok) {
            throw new Error('Failed to fetch finances');
        }
        if (!departmentStatsResponse.ok) {
            throw new Error('Failed to fetch department statistics');
        }

        const employees = await employeesResponse.json();
        const leaves = await leavesResponse.json();
        const tasks = await tasksResponse.json();
        const finances = await financesResponse.json();
        const departmentStats = await departmentStatsResponse.json(); // Get department stats

        // Count leaves with status "Pending"
        const pendingLeavesCount = leaves.filter(leave => leave.status === 'Pending').length;

        // Count tasks with status "Not Started"
        const notStartedTasksCount = tasks.filter(task => task.status === 'Not Started').length;

        // Count finances with status "Pending"
        const pendingFinancesCount = finances.filter(finance => finance.status === 'Pending').length;

        // Update the overview section
        document.getElementById('totalEmployees').innerText = employees.length;
        document.getElementById('totalLeaves').innerText = pendingLeavesCount; // Update to show only pending leaves
        document.getElementById('totalTasks').innerText = notStartedTasksCount; // Update to show only tasks not started
        document.getElementById('totalFinanceRecords').innerText = pendingFinancesCount; // Update to show only pending finances

        // Call function to render the pie chart
        renderDepartmentChart(departmentStats);
    } catch (error) {
        console.error('Error fetching overview data:', error);
        alert('Error fetching overview data: ' + error.message);
    }
}

let departmentChart; // Declare a variable to hold the chart instance

function renderDepartmentChart(departments) {
    // Filter out departments with null or empty names
    const filteredDepartments = departments.filter(department =>
        department.departmentName && department.departmentName.trim() !== ''
    );

    const departmentNames = filteredDepartments.map(department => department.departmentName);
    const departmentCounts = filteredDepartments.map(department => department.employeeCount);

    Highcharts.chart('departmentChart', {
        chart: {
            type: 'pie',
            options3d: {
                enabled: true,
                alpha: 10,
                beta: 10,
                depth: 50
            }
        },
        title: {
            text: 'Employee Distribution by Department'
        },
        plotOptions: {
            pie: {
                innerSize: 100,
                depth: 45,
                dataLabels: {
                    enabled: true,
                    format: '{point.name}: {point.y} ({point.percentage:.1f}%)'
                }
            }
        },
        series: [{
            name: 'Employees',
            data: departmentNames.map((name, index) => [name, departmentCounts[index]])
        }]
    });
}
function validateEmployeeData(employeeData) {
    const errors = [];

    // Clear previous error messages
    document.getElementById('employeeNameError').innerText = '';
    document.getElementById('employeeLastNameError').innerText = '';
    document.getElementById('employeeEmailError').innerText = '';
    document.getElementById('employeePasswordError').innerText = '';
    document.getElementById('employeePhoneNumberError').innerText = '';
    document.getElementById('employeeJoiningDateError').innerText = '';
    document.getElementById('emergencyContactNameError').innerText = '';
    document.getElementById('emergencyContactNumberError').innerText = '';
    document.getElementById('updateEmployeeNameError').innerText = '';
    document.getElementById('updateEmployeeLastNameError').innerText = '';
    document.getElementById('updateEmployeePhoneNumberError').innerText = '';
    document.getElementById('updateEmergencyContactNameError').innerText = '';
    document.getElementById('updateEmergencyContactNumberError').innerText = '';

    // Validate first name
    if (!/^[A-Za-z\s'-]+$/.test(employeeData.firstName)) {
        document.getElementById('employeeNameError').innerText = 'First name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
        document.getElementById('updateEmployeeNameError').innerText = 'First name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
    }

    // Validate last name
    if (!/^[A-Za-z\s'-]+$/.test(employeeData.lastName)) {
        document.getElementById('employeeLastNameError').innerText = 'Last name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
        document.getElementById('updateEmployeeLastNameError').innerText = 'Last name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
    }

    // Validate emergency contact name
    if (!/^[A-Za-z\s'-]+$/.test(employeeData.emergencyContactName)) {
        document.getElementById('emergencyContactNameError').innerText = 'Emergency contact name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
        document.getElementById('updateEmergencyContactNameError').innerText = 'Emergency contact name must contain only alphabetic characters, spaces, hyphens, or apostrophes.';
    }

    // Validate email format (only for create employee)
    if (employeeData.email && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(employeeData.email)) {
        document.getElementById('employeeEmailError').innerText = 'Invalid email format.';
    }


    // Validate phone number format
    const phoneRegex = /^(\+254(10[0-9]|11[0-9]|7[0-9]{8})|07[0-9]{8}|010[0-9]{8}|011[0-9]{8})$/;
    if (!phoneRegex.test(employeeData.phoneNumber)) {
        document.getElementById('employeePhoneNumberError').innerText = 'Phone number format should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.';
        document.getElementById('updateEmployeePhoneNumberError').innerText = 'Phone number format should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.';
    }

    // Validate emergency contact phone number format
    if (!phoneRegex.test(employeeData.emergencyContactPhone)) {
        document.getElementById('emergencyContactNumberError').innerText = 'Emergency contact phone number format should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.';
        document.getElementById('updateEmergencyContactNumberError').innerText = 'Emergency contact phone number format should start with +254 10, +254 11, +254 7, 07, 010 or 011 followed by 8 digits.';
    }

    // Validate date of joining
    if (new Date(employeeData.dateOfJoining) > new Date()) {
        document.getElementById('employeeJoiningDateError').innerText = 'Date of joining must not be in the future.';
    }

    // Validate password complexity
    const passwordPattern = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;
    if (!passwordPattern.test(employeeData.password)) {
        document.getElementById('employeePasswordError').innerText = 'Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character.';
    }

    // Check if there are any errors
    return errors.length > 0;
}
function showCreateEmployeeForm() {
    resetForm(); // Reset the form fields when showing the form
    const createEmployeeFormContainer = document.getElementById('createEmployeeFormContainer');

    if (createEmployeeFormContainer.classList.contains('hidden')) {
        createEmployeeFormContainer.classList.remove('hidden'); // Show the form
    } else {
        createEmployeeFormContainer.classList.add('hidden'); // Hide the form
    }
}
// Function to create an employee
async function createEmployee() {
    const employeeData = {
        firstName: document.getElementById('employeeName').value,
        lastName: document.getElementById('employeeLastName').value,
        email: document.getElementById('employeeEmail').value,
        password: document.getElementById('employeePassword').value,
        role: document.getElementById('employeeRole').value,
        phoneNumber: document.getElementById('employeePhoneNumber').value,
        department: document.getElementById('employeeDepartment').value,
        dateOfJoining: document.getElementById('employeeJoiningDate').value, // New field
        emergencyContactName: document.getElementById('emergencyContactName').value, // New field
        emergencyContactPhone: document.getElementById('emergencyContactNumber').value // Updated field name
    };

    // Validate inputs
    const validationErrors = validateEmployeeData(employeeData);
    if (validationErrors.length > 0) {
        alert('Please fix the following errors:\n' + validationErrors.join('\n'));
        return; // Stop execution if there are validation errors
    }

    try {
        const response = await fetch(`${BASE_URL}/employees`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(employeeData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to create employee: ${errorText}`);
        }

        const newEmployee = await response.json();
        console.log('Employee created:', newEmployee);
        alert('Employee created successfully!');

        // Check if the contact already exists before creating it
        const contactResponse = await fetch(`${BASE_URL}/contacts`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                employee: { employeeId: newEmployee.employeeId },
                name: `${employeeData.firstName} ${employeeData.lastName}`,
                email: employeeData.email,
                phoneNumber: employeeData.phoneNumber,
                department: employeeData.department,
                emergencyContactName: employeeData.emergencyContactName,
                emergencyContactPhone: employeeData.emergencyContactPhone
            })
        });

        if (!contactResponse.ok) {
            const contactErrorText = await contactResponse.text();
            console.warn(`Contact creation failed: ${contactErrorText}`);
            alert('Contact created successfully!');
        } else {
            console.log('Contact created successfully!');
        }


        resetForm(); // Reset the form after creation
        getAllEmployees(); // Refresh the employee list
        getAllContacts(); // Refresh the contact list
        fetchOverviewData();

        // Hide the create employee form after successful creation
        showCreateEmployeeForm(); // This will toggle the form visibility
    } catch (error) {
        console.error('Error creating employee:', error);
        alert('Error creating employee: ' + error.message);
    }
}

// Function to set the max date to today
function setMaxDate() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0'); // January is 0!
    const dd = String(today.getDate()).padStart(2, '0');
    const maxDate = `${yyyy}-${mm}-${dd}`;
    document.getElementById('employeeJoiningDate').setAttribute('max', maxDate);
}

// Call the function when the page loads
window.onload = setMaxDate;

// Function to create a contact for the employee
async function createContact(employeeId) {
    const contactData = {
        employee: { employeeId: employeeId }, // Assuming your backend expects this
        name: `${document.getElementById('employeeName').value} ${document.getElementById('employeeLastName').value}`,
        email: document.getElementById('employeeEmail').value,
        phoneNumber: document.getElementById('employeePhoneNumber').value,
        department: document.getElementById('employeeDepartment').value,
        emergencyContactName: document.getElementById('emergencyContactName').value, // New field
        emergencyContactPhone: document.getElementById('emergencyContactNumber').value // New field
    };

    try {
        const response = await fetch(`${BASE_URL}/contacts`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(contactData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to create contact: ${errorText}`);
        }

        console.log('Contact created successfully!');
    } catch (error) {
        console.error('Error creating contact:', error);
        alert('Error creating contact: ' + error.message);
    }
}

// Function to reset the form
function resetForm() {
    document.getElementById('createEmployeeForm').reset(); // Reset the form fields
    const createButton = document.querySelector('#createEmployeeForm button');
    createButton.innerText = 'Create Employee'; // Reset button text
    createButton.setAttribute('onclick', 'createEmployee()'); // Reset button functionality
}

// Function to get all employees and populate the employee table
async function getAllEmployees() {
    try {
        const response = await fetch(`${BASE_URL}/employees`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch employees');
        }

        const employees = await response.json();
        console.log('Employees:', employees);
        populateEmployeeTable(employees);
    } catch (error) {
        console.error('Error fetching employees for table:', error);
    }
}
// Function to populate the employee table
function populateEmployeeTable(employees) {
    const employeeTableBody = document.getElementById('employeeTableBody');
    employeeTableBody.innerHTML = ''; // Clear existing rows

    employees.forEach(employee => {
        // Exclude the default admin account
        if (employee.email === "admin@example.com") {
            return; // Skip this iteration
        }

        const statusClass = employee.active ? 'text-success' : 'text-danger'; // Use Bootstrap classes for coloring
        const statusText = employee.active ? 'Active' : 'Inactive';

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${employee.firstName} ${employee.lastName}</td>
            <td>${employee.email}</td>
            <td>${employee.role}</td>
            <td>${employee.phoneNumber}</td>
            <td>${employee.department}</td>
            <td>${employee.dateOfJoining}</td> <!-- New field -->
            <td>${employee.emergencyContactName}</td> <!-- New field -->
            <td>${employee.emergencyContactPhone}</td> <!-- New field -->
            <td class="${statusClass}">${statusText}</td> <!-- Apply status class -->
            <td>
                <button class="btn btn-warning" onclick="editEmployee('${employee.employeeId}')">Edit</button>
                <button class="btn btn-danger" onclick="deactivateEmployee('${employee.employeeId}')">Deactivate</button>
            </td>
        `;
        employeeTableBody.appendChild(row);
    });
}

// Function to edit an employee
async function editEmployee(empId) {
    console.log('Editing employee with ID:', empId); // Debugging log
    try {
        const response = await fetch(`${BASE_URL}/employees/${empId}`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch employee details');
        }

        const employee = await response.json();
        console.log('Fetched Employee:', employee); // Debugging log

        // Populate the form with the employee's current details
        document.getElementById('updateEmployeeName').value = employee.firstName;
        document.getElementById('updateEmployeeLastName').value = employee.lastName;
        document.getElementById('updateEmployeePhoneNumber').value = employee.phoneNumber;
        document.getElementById('updateEmergencyContactName').value = employee.emergencyContactName;
        document.getElementById('updateEmergencyContactNumber').value = employee.emergencyContactPhone;
        document.getElementById('updateEmployeeDepartment').value = employee.department;

        // Store the employee ID in a hidden field or a global variable
        window.currentEmployeeId = empId; // Store the employee ID globally

        // Show the update modal
        $('#updateEmployeeModal').modal('show'); // Using jQuery to show the modal
    } catch (error) {
        console.error('Error fetching employee details:', error);
        alert('Error fetching employee details: ' + error.message);
    }
}

// Function to update an employee
async function updateEmployee() {
    const empId = window.currentEmployeeId; // Get the employee ID from the global variable
    const employeeData = {
        firstName: document.getElementById('updateEmployeeName').value,
        lastName: document.getElementById('updateEmployeeLastName').value,
        phoneNumber: document.getElementById('updateEmployeePhoneNumber').value,
        emergencyContactName: document.getElementById('updateEmergencyContactName').value,
        emergencyContactPhone: document.getElementById('updateEmergencyContactNumber').value,
        department: document.getElementById('updateEmployeeDepartment').value,
        dateOfJoining: document.getElementById('employeeJoiningDate').value // Ensure this is included
    };

    // Validate inputs
    const validationErrors = validateEmployeeData(employeeData);
    if (validationErrors.length > 0) {
        alert('Please fix the following errors:\n' + validationErrors.join('\n'));
        return; // Stop execution if there are validation errors
    }

    try {
        const response = await fetch(`${BASE_URL}/employees/${empId}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(employeeData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to update employee: ${errorText}`);
        }

        const updatedEmployee = await response.json();
        alert('Employee updated successfully!');
        $('#updateEmployeeModal').modal('hide'); // Hide the modal after updating
        getAllEmployees(); // Refresh the employee list
        fetchOverviewData(); // Update the overview
    } catch (error) {
        console.error('Error updating employee:', error);
        alert('Error updating employee: ' + error.message);
    }
}
// Function to reset an employee's password
async function resetEmployeePassword() {
    const employeeEmail = document.getElementById('resetEmployeeEmail').value.trim();
    const newPassword = document.getElementById('newPassword').value.trim();

    // Input validation
    if (!employeeEmail) {
        alert('Please enter a valid Employee Email.');
        return;
    }
    if (!newPassword) {
        alert('Please enter a new password.');
        return;
    }

    // Show loading indicator (optional)
    const loadingMessage = document.createElement('div');
    loadingMessage.innerText = 'Resetting password...';
    document.body.appendChild(loadingMessage);

    try {
        const response = await fetch(`${BASE_URL}/employees/reset-password?email=${encodeURIComponent(employeeEmail)}&newPassword=${encodeURIComponent(newPassword)}`, {
            method: 'PUT',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to reset password: ${errorText}`);
        }

        alert('Password reset successfully!');
        document.getElementById('resetPasswordForm').reset(); // Reset the form after successful reset
    } catch (error) {
        console.error('Error resetting password:', error);
        alert('Error resetting password: ' + error.message);
    } finally {
        // Remove loading indicator
        if (loadingMessage) {
            document.body.removeChild(loadingMessage);
        }
    }
}

// Function to close the update modal
function closeUpdateModal() {
    $('#updateEmployeeModal').modal('hide'); // Hide the modal
}

async function deactivateEmployee(empId) {
    try {
        const response = await fetch(`${BASE_URL}/employees/${empId}/deactivate`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to deactivate employee: ${errorText}`);
        }

        const message = await response.text();
        console.log(message);
        alert('Employee deactivated successfully!');
        getAllEmployees(); // Refresh the employee list to reflect the status change
    } catch (error) {
        console.error('Error deactivating employee:', error);
        alert('Error deactivating employee: ' + error.message);
    }
}

// Function to get all contacts
async function getAllContacts() {
    try {
        const response = await fetch(`${BASE_URL}/contacts`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch contacts');
        }

        const contacts = await response.json();
        console.log('Contacts:', contacts);
        populateContactCards(contacts); // Call the updated function to populate cards
    } catch (error) {
        console.error('Error fetching contacts:', error);
    }
}

// Function to populate the contact cards
function populateContactCards(contacts) {
    const contactCardsContainer = document.getElementById('contactCardsContainer');
    contactCardsContainer.innerHTML = ''; // Clear existing cards

    contacts.forEach(contact => {
        const card = document.createElement('div');
        card.className = 'card mb-3'; // Bootstrap class for card styling
        card.style.width = '18rem'; // Set a fixed width for the cards
        card.innerHTML = `
            <div class="card-body">
                <h5 class="card-title">${contact.name}</h5>
                <p class="card-text">Email: ${contact.email}</p>
                <p class="card-text">Phone Number: ${contact.phoneNumber}</p>
                <p class="card-text">Department: ${contact.department}</p>
                <p class="card-text">
                    Status: <span class="${contact.active ? 'text-success' : 'text-danger'}">
                    ${contact.active ? 'Active' : 'Inactive'}</span>
                </p>
            </div>
        `;
        contactCardsContainer.appendChild(card);
    });
}

// Logout function
function logout() {
    console.log('Logout function called');
    localStorage.removeItem('userToken');
    window.location.href = '/';
}

// Event listeners
document.getElementById('createEmployeeForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent form submission
    createEmployee();
});

            //FINANCE
// Fetch and display finance records for admin
async function fetchAdminFinanceRecords() {
    try {
        const response = await fetch(`${BASE_URL}/finances`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch finance records');
        }

        const allFinances = await response.json();

        // Filter active records (e.g., Pending, Recalled)
        const activeFinances = allFinances.filter(finance =>
            finance.status === 'Pending'
        );

        const recordsBody = document.getElementById('admin-finance-records-body');
        recordsBody.innerHTML = ''; // Clear existing rows

        activeFinances.forEach(finance => {
            const row = createFinanceRecordRow(finance);
            recordsBody.appendChild(row);
        });


    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching finance records: ' + error.message);
    }
}

// Create a row for finance record
function createFinanceRecordRow(finance) {
    const row = document.createElement('tr');
    row.setAttribute('data-finance-id', finance.financeId);

    const isDeleted = finance.isDeleted;
    const isRequisition = finance.type === 'Requisition'; // Check if it's a requisition

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
            <div class="dropdown">
                <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Actions
                </button>
                <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    <button class="dropdown-item" onclick="handleAdminAction('${finance.financeId}', 'approve')" ${isDeleted ? 'disabled' : ''}>Approve</button>
                    <button class="dropdown-item" onclick="handleAdminAction('${finance.financeId}', 'reject')" ${isDeleted ? 'disabled' : ''}>Reject</button>
                    <button class="dropdown-item" onclick="handleAdminAction('${finance.financeId}', 'delete')">Delete</button>
                    ${!isRequisition ? `<button class="dropdown-item" onclick="downloadFinanceFile('${finance.financeId}')">Download</button>` : ''}
                </div>
            </div>
        </td>
    `;

    if (isDeleted) {
        row.style.textDecoration = 'line-through';
        row.style.color = 'gray';
    }

    return row;
}

// Fetch and display finance history
async function fetchFinanceHistory() {
    try {
        const response = await fetch(`${BASE_URL}/finances`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch finance history');
        }

        const allFinances = await response.json();

        // Filter historical records (e.g., Approved, Rejected, Deleted)
        const historicalFinances = allFinances.filter(finance =>
            finance.status === 'Approved' || finance.status === 'Rejected' || finance.status === 'Deleted'
        );

        const historyBody = document.getElementById('finance-history-body');
        historyBody.innerHTML = ''; // Clear previous history

        historicalFinances.forEach(finance => {
            const row = createFinanceHistoryRow(finance);
            historyBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching finance history: ' + error.message);
    }
}

// Create a row for finance history
function createFinanceHistoryRow(finance) {
    const row = document.createElement('tr');
    const isRequisition = finance.type === 'Requisition'; // Check if it's a requisition

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
            ${!isRequisition ? `<button onclick="downloadFinanceFile('${finance.financeId}')" class="btn btn-primary">Download</button>` : ''}
        </td>
    `;
    return row;
}

// Function to handle admin actions
async function handleAdminAction(financeId, action) {
    try {
        let response;

        // Fetch the current status of the finance record
        const financeResponse = await fetch(`${BASE_URL}/finances/${financeId}`, {
            headers: getAuthHeaders()
        });

        if (!financeResponse.ok) {
            throw new Error('Failed to fetch finance record');
        }

        const financeRecord = await financeResponse.json();

        if (action === 'delete') {
            // Use DELETE method for deletion
            response = await fetch(`${BASE_URL}/finances/${financeId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Failed to delete finance record');
            }

            alert(`Finance record deleted successfully!`);
        } else {
            // Check if the record is already approved or rejected
            if (action === 'approve' && financeRecord.status === 'Approved') {
                alert('This finance record has already been approved and cannot be approved again.');
                return;
            }
            if (action === 'reject' && financeRecord.status === 'Rejected') {
                alert('This finance record has already been rejected and cannot be rejected again.');
                return;
            }

            // Use PATCH method for approve, reject, recall
            response = await fetch(`${BASE_URL}/finances/${financeId}/${action}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders()
                },
                body: JSON.stringify("Feedback or reason for action") // Optional feedback
            });

            if (!response.ok) {
                throw new Error(`Failed to ${action} finance record`);
            }

            alert(`Finance record ${action} successfully!`);
        }

        // Refresh both tables after the action
        await fetchAdminFinanceRecords();
        await fetchFinanceHistory();
    } catch (error) {
        console.error('Error:', error);
        alert(`Error during admin action: ${error.message}`);
    }
}

// Fetch and display deleted finance records
async function fetchDeletedFinanceRecords() {
    try {
        const response = await fetch(`${BASE_URL}/finances/deleted`, {
            headers: getAuthHeaders() // Use the getAuthHeaders function
        });

        if (!response.ok) {
            throw new Error('Failed to fetch deleted finance records');
        }

        const deletedFinances = await response.json();
        const deletedRecordsBody = document.getElementById('deleted-finance-records-body');
        deletedRecordsBody.innerHTML = ''; // Clear existing rows

        deletedFinances.forEach(finance => {
            const row = createDeletedFinanceRecordRow(finance);
            deletedRecordsBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error fetching deleted finance records: ' + error.message);
    }
}

// Create a row for deleted finance record
function createDeletedFinanceRecordRow(finance) {
    const row = document.createElement('tr');
    const isRequisition = finance.type === 'Requisition'; // Check if it's a requisition

    row.innerHTML = `
        <td>${finance.type}</td>
        <td>${finance.purpose || finance.expenseType || '-'}</td>
        <td>$${parseFloat(finance.amount).toFixed(2)}</td>
        <td>${new Date(finance.dateSubmitted).toLocaleDateString()}</td>
        <td>
            <span class="status-badge deleted">
                Deleted <span class="text-danger" title="Deleted" style="margin-left: 5px;">&#10060;</span>
            </span>
        </td>
        <td>
            <button onclick="restoreFinanceRecord('${finance.financeId}')" class="btn btn-primary">Restore</button>
            ${!isRequisition ? `<button onclick="downloadFinanceFile('${finance.financeId}')" class="btn btn-secondary">Download</button>` : ''}
        </td>
    `;
    row.style.textDecoration = 'line-through'; // Strikethrough effect
    row.style.color = 'gray'; // Optional: Change text color
    return row;
}

// Function to restore a deleted finance record
async function restoreFinanceRecord(financeId) {
    try {
        const response = await fetch(`${BASE_URL}/finances/${financeId}/restore`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            }
        });

        if (!response.ok) {
            throw new Error('Failed to restore finance record');
        }

        alert('Finance record restored successfully!');

        // Fetch the updated record to determine where it should go
        const financeResponse = await fetch(`${BASE_URL}/finances/${financeId}`, {
            headers: getAuthHeaders()
        });

        if (!financeResponse.ok) {
            throw new Error('Failed to fetch restored finance record');
        }

        const financeRecord = await financeResponse.json();

        if (financeRecord.status === 'Pending' || financeRecord.status === 'Recalled') {
            await fetchAdminFinanceRecords(); // Refresh the admin finance records table
        } else {
            await fetchFinanceHistory(); // Refresh the finance history table
        }

        await fetchDeletedFinanceRecords(); // Refresh the deleted records table
    } catch (error) {
        console.error('Error:', error);
        alert('Error restoring finance record: ' + error.message);
    }
}
// Function to download the supporting document associated with a finance record
function downloadFinanceFile(financeId) {
    fetch(`${BASE_URL}/finances/${financeId}/download`, {
        method: 'GET',
        headers: getAuthHeaders() // Use the getAuthHeaders function
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to download the file');
            }
            return response.blob(); // Convert the response to a Blob
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob); // Create a URL for the Blob
            const a = document.createElement('a'); // Create an anchor element
            a.href = url; // Set the href to the Blob URL
            a.download = `finance_record_${financeId}.pdf`; // Set the download attribute
            document.body.appendChild(a); // Append the anchor to the body
            a.click(); // Programmatically click the anchor to trigger the download
            a.remove(); // Remove the anchor from the document
            window.URL.revokeObjectURL(url); // Release the Blob URL
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error downloading file: ' + error.message);
        });
}
                //LEAVE
// Function to fetch and display all leave requests
function fetchLeaveRequests() {
    fetch(`${BASE_URL}/leaves`, { // Fetch all leave requests
        method: 'GET',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(leaves => {
            console.log('Fetched leave requests:', leaves); // Debugging: Check fetched data
            const leaveTableBody = document.getElementById('leaveTableBody');
            leaveTableBody.innerHTML = ''; // Clear existing rows

            // Filter leave requests to show only those with a 'Pending' status
            const pendingLeaves = leaves.filter(leave => leave.status === 'Pending');
            console.log('Pending leave requests:', pendingLeaves); // Debugging: Check filtered data

            pendingLeaves.forEach(leave => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${leave.employeeName}</td>
                    <td>${leave.leaveType}</td>
                    <td>${leave.startDate}</td>
                    <td>${leave.endDate}</td>
                    <td>${leave.status}</td>
                    <td>
                        <button class="btn btn-success" onclick="approveLeave('${leave.leaveId}')">Approve</button>
                        <button class="btn btn-danger" onclick="rejectLeave('${leave.leaveId}')">Reject</button>
                    </td>
                `;
                leaveTableBody.appendChild(row);
            });
        })
        .catch(error => console.error('Error fetching leave records:', error));
}
// Function to fetch and display leave history
function fetchLeaveHistory() {
    fetch(`${BASE_URL}/leaves/history`, { // Fetch all leave history
        method: 'GET',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(leaves => {
            const leaveHistoryTableBody = document.getElementById('leaveHistoryTableBody');
            leaveHistoryTableBody.innerHTML = ''; // Clear existing rows

            // Filter leave requests to exclude those with a 'pending' status
            const historyLeaves = leaves.filter(leave => leave.status !== 'Pending');

            historyLeaves.forEach(leave => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${leave.employeeName}</td>
                    <td>${leave.leaveType}</td>
                    <td>${leave.startDate}</td>
                    <td>${leave.endDate}</td>
                    <td>${leave.status}</td>
                    <td>${leave.dateRequested || ''}</td> <!-- Assuming there's a dateRequested field -->
                    <td>
                        ${leave.status === 'Approved' ?
                    `<button class="btn btn-warning" onclick="recallLeave('${leave.leaveId}')">Recall</button>` :
                    ''
                } <!-- Recall Button -->
                    </td>
                `;
                leaveHistoryTableBody.appendChild(row);
            });
        })
        .catch(error => console.error('Error fetching leave history:', error));
}

// Function to approve a leave request
function approveLeave(leaveId) {
    fetch(`${BASE_URL}/leaves/${leaveId}/approve`, {
        method: 'PUT',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            console.log('Leave approved successfully');
            fetchLeaveRequests(); // Refresh the pending leave requests list
            fetchLeaveHistory(); // Refresh the leave history list
        })
        .catch(error => console.error('Error approving leave request:', error));
}

function rejectLeave(leaveId) {
    fetch(`${BASE_URL}/leaves/${leaveId}/reject`, {
        method: 'PUT',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            console.log('Leave rejected successfully');
            fetchLeaveRequests(); // Refresh the pending leave requests list
            fetchLeaveHistory(); // Refresh the leave history list
        })
        .catch(error => console.error('Error rejecting leave request:', error));
}

// Function to recall a leave request
function recallLeave(leaveId) {
    fetch(`${BASE_URL}/leaves/${leaveId}/recall`, {
        method: 'PUT',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            console.log('Leave recalled successfully');
            fetchLeaveRequests(); // Refresh the pending leave requests list
            fetchLeaveHistory(); // Refresh the leave history list
        })
        .catch(error => console.error('Error recalling leave request:', error));
}

// Function to populate the employee dropdown
async function populateEmployeeDropdown() {
    try {
        const response = await fetch(`${BASE_URL}/employees`, {
            method: 'GET',
            headers: getAuthHeaders()
        });
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const employees = await response.json();
        const assignedToSelect = document.getElementById('assignedTo');

        employees.forEach(employee => {
            const option = document.createElement('option');
            option.value = employee.employeeId; // Use employee ID as the value
            option.textContent = employee.fullName; // Display employee name
            assignedToSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error fetching employees:', error);
    }
}

//TASK
// Function to get all tasks (Admin only)
async function getAllTasks() {
    try {
        const response = await fetch(`${BASE_URL}/tasks`, {
            method: 'GET',
            headers: getAuthHeaders()
        });
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const tasks = await response.json();
        populateTaskTable(tasks);
    } catch (error) {
        console.error('Error fetching tasks:', error);
    }
}

// Function to populate task table
function populateTaskTable(tasks) {
    const taskTableBody = document.getElementById('taskTableBody');
    taskTableBody.innerHTML = ''; // Clear existing rows

    tasks.forEach(task => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${task.taskName}</td>
            <td>${task.assignedToName}</td>
            <td>${task.assignedByName}</td>
            <td>${task.description}</td>
            <td>${task.dueDate}</td>
            <td>${task.status}</td>
            <td>${task.urgent ? 'Yes' : 'No'}</td>
            <td>
                <div class="dropdown">
                    <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Actions
                    </button>
                    <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                        <button class="dropdown-item btn-info" onclick="populateTaskDetails('${task.taskId}')">View</button>
                        <button class="dropdown-item btn-warning" onclick="populateUpdateForm('${task.taskId}')">Update</button>
                        <button class="dropdown-item btn-danger" onclick="deleteTask('${task.taskId}')">Delete</button>
                    </div>
                </div>
            </td>
        `;
        taskTableBody.appendChild(row);
    });
}

// Function to populate task details for viewing
function populateTaskDetails(taskId) {
    fetch(`${BASE_URL}/tasks/${taskId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(task => {
            // Populate the modal with task details
            document.getElementById('modalTaskName').textContent = task.taskName;
            document.getElementById('modalDescription').textContent = task.description;
            document.getElementById('modalAssignedTo').textContent = task.assignedToName;
            document.getElementById('modalAssignedBy').textContent = task.assignedByName;
            document.getElementById('modalDueDate').textContent = task.dueDate;
            document.getElementById('modalStatus').textContent = task.status;
            document.getElementById('modalUrgent').textContent = task.urgent ? 'Yes' : 'No';

            // Show the modal
            $('#taskDetailModal').modal('show');
        })
        .catch(error => console.error('Error fetching task details:', error));
}


function populateUpdateForm(taskId) {
    fetch(`${BASE_URL}/tasks/${taskId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(task => {
            // Show the form
            document.getElementById('createTaskForm').style.display = 'block';

            // Populate the form fields with the task details
            const taskNameInput = document.getElementById('taskName');
            const descriptionInput = document.getElementById('description');
            const dueDateInput = document.getElementById('dueDate');
            const statusSelect = document.getElementById('status');
            const assignedToSelect = document.getElementById('assignedTo');
            const urgentCheckbox = document.getElementById('urgent');

            if (taskNameInput) taskNameInput.value = task.taskName;
            if (descriptionInput) descriptionInput.value = task.description;
            if (dueDateInput) dueDateInput.value = task.dueDate;
            if (statusSelect) {
                statusSelect.value = task.status;
                statusSelect.disabled = true; // Disable the status field for admin
            }
            if (assignedToSelect) assignedToSelect.value = task.assignedTo.employeeId; // Assuming assignedTo is an object
            if (urgentCheckbox) urgentCheckbox.checked = task.urgent;

            // Change the button to update the task
            const createButton = document.querySelector('button[onclick="createTask()"]');
            if (createButton) {
                createButton.textContent = 'Update Task';
                createButton.setAttribute('onclick', `updateTask('${taskId}')`);
            } else {
                console.error('Create button not found');
            }
        })
        .catch(error => console.error('Error fetching task details for update:', error));
}

function updateTask(taskId) {
    const dueDateInput = document.getElementById('dueDate').value;
    const today = new Date().toISOString().split('T')[0]; // Get today's date in YYYY-MM-DD format

    if (dueDateInput < today) {
        alert('Due date cannot be in the past. Please select a valid date.');
        return; // Exit the function if the date is invalid
    }

    const taskDetails = {
        taskName: document.getElementById('taskName').value,
        assignedTo: { employeeId: document.getElementById('assignedTo').value },
        description: document.getElementById('description').value,
        dueDate: dueDateInput,
        urgent: document.getElementById('urgent').checked
    };

    fetch(`${BASE_URL}/tasks/${taskId}`, {
        method: 'PUT', // Use PUT for updating
        headers: {
            ...getAuthHeaders(),
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(taskDetails)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(updatedTask => {
            console.log('Task updated successfully:', updatedTask);
            getAllTasks(); // Refresh the task list
            resetCreateTaskForm(); // Reset the form after update
            $('#createTaskForm').hide(); // Hide the create task form
            $('#taskDetailModal').modal('hide'); // Close the modal after task update
        })
        .catch(error => console.error('Error updating task:', error));
}

function createTask() {
    const taskName = document.getElementById('taskName').value;
    const taskNameRegex = /^[a-zA-Z0-9\s.,!?'"()\-;:]+$/; // Updated regex for validation

    if (!taskNameRegex.test(taskName)) {
        alert('Task name is invalid. Please use only alphanumeric characters, spaces, and common punctuation.');
        return; // Exit the function if the task name is invalid
    }

    const taskDetails = {
        taskName: taskName,
        assignedTo: { employeeId: document.getElementById('assignedTo').value },
        description: document.getElementById('description').value,
        dueDate: document.getElementById('dueDate').value,
        status: 'Not Started', // Set default status
        urgent: document.getElementById('urgent').checked
    };

    fetch(`${BASE_URL}/tasks`, {
        method: 'POST',
        headers: {
            ...getAuthHeaders(),
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(taskDetails)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(newTask => {
            console.log('Task created successfully:', newTask);
            getAllTasks(); // Refresh the list
            resetCreateTaskForm(); // Reset the form after creation
            $('#createTaskForm').hide(); // Hide the create task form
        })
        .catch(error => console.error('Error creating task:', error));
}

// Function to delete a task
function deleteTask(taskId) {
    if (confirm('Are you sure you want to delete this task?')) {
        fetch(`${BASE_URL}/tasks/${taskId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                console.log('Task deleted successfully');
                getAllTasks(); // Refresh the list
            })
            .catch(error => console.error('Error deleting task:', error));
    }
}

function resetCreateTaskForm() {
    const taskNameInput = document.getElementById('taskName');
    const descriptionInput = document.getElementById('description');
    const dueDateInput = document.getElementById('dueDate');
    const statusSelect = document.getElementById('status');
    const assignedToSelect = document.getElementById('assignedTo');
    const urgentCheckbox = document.getElementById('urgent');

    if (taskNameInput) taskNameInput.value = '';
    if (descriptionInput) descriptionInput.value = '';
    if (dueDateInput) dueDateInput.value = '';
    if (statusSelect) statusSelect.value = 'not_started'; // Reset to default
    if (assignedToSelect) assignedToSelect.value = ''; // Reset to default
    if (urgentCheckbox) urgentCheckbox.checked = false; // Reset checkbox

    // Reset the button to create
    const createButton = document.querySelector('button[onclick^="updateTask"]');
    if (createButton) {
        createButton.textContent = 'Create Task';
        createButton.setAttribute('onclick', 'createTask()'); // Reset the button to create
    } else {
        console.error('Create button not found');
    }
}

// Function to open the create task modal
function openCreateTaskModal() {
    $('#createTaskForm').show(); // Show the create task form
    setMinDueDate(); // Ensure the minimum date is set when opening the modal
}

// Set the minimum date for the due date input
function setMinDueDate() {
    const today = new Date();
    const formattedDate = today.toISOString().split('T')[0]; // Format as YYYY-MM-DD
    document.getElementById('dueDate').setAttribute('min', formattedDate);
}

// Call the function to set the minimum date when the page loads
window.onload = setMinDueDate;


// NOTIFICATIONS
let stompClient = null;
let notificationCount = 0;
let notifications = []; // Store notifications in an array

function connectWebSocket() {
    const token = localStorage.getItem('jwt'); // Retrieve the JWT from local storage
    const socket = new SockJS(`http://localhost:8082/notifications?token=${token}`); // Include token in the URL
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        // Subscribe to the admin notifications topic
        stompClient.subscribe('/topic/admins', function (notification) {
            handleNotification(JSON.parse(notification.body));
        });
    }, function (error) {
        console.error('WebSocket connection error:', error);
    });
}

function handleNotification(notification) {
    console.log('New notification:', notification); // Debugging log

    // Check if notification has the expected structure
    if (!notification.message || !notification.timestamp) {
        console.error('Notification does not have the required properties:', notification);
        return;
    }

    // Check for duplicates
    const exists = notifications.some(n => n.message === notification.message && n.timestamp === notification.timestamp);
    if (exists) {
        console.log('Duplicate notification, not adding:', notification);
        return; // Exit if it's a duplicate
    }

    // Add the notification to the notifications array with a read status
    notifications.push({ ...notification, read: false });
    saveNotifications(); // Save notifications to localStorage

    // Display the notification
    displayNotifications();
}

// Function to display notifications
function displayNotifications() {
    const notificationDisplayArea = document.getElementById('notificationDisplayArea');
    notificationDisplayArea.innerHTML = ''; // Clear existing notifications

    notifications.forEach((notification, index) => {
        const notificationItem = document.createElement('div');
        notificationItem.className = `alert alert-info ${notification.read ? 'read' : ''}`; // Add a class if read
        notificationItem.innerText = `${notification.message} (Received at: ${new Date(notification.timestamp).toLocaleString()})`; // Display message and timestamp

        // Only add the button if the notification is not read
        if (!notification.read) {
            const markAsReadButton = document.createElement('button');
            markAsReadButton.className = 'btn btn-success btn-sm float-right';
            markAsReadButton.innerText = 'Mark as Read';
            markAsReadButton.onclick = function () {
                markNotificationAsRead(index);  // Mark this notification as read
            };

            notificationItem.appendChild(markAsReadButton); // Append the button
        }

        notificationDisplayArea.appendChild(notificationItem); // Append the notification item
    });

    // Update the notification count
    notificationCount = notifications.filter(n => !n.read).length; // Count unread notifications
    document.getElementById('notificationCount').innerText = notificationCount;
}

// Function to mark a specific notification as read
function markNotificationAsRead(index) {
    notifications[index].read = true; // Set the read status to true
    saveNotifications(); // Save notifications to localStorage
    displayNotifications(); // Refresh the display
}

// Function to save notifications to localStorage
function saveNotifications() {
    localStorage.setItem('notifications', JSON.stringify(notifications));
}

// Function to load notifications from localStorage
function loadNotifications() {
    const savedNotifications = localStorage.getItem('notifications');
    if (savedNotifications) {
        notifications = JSON.parse(savedNotifications);
    }
}

// Mark all notifications as read
document.getElementById('markAllAsRead').addEventListener('click', function () {
    notifications.forEach(notification => notification.read = true); // Mark all as read
    saveNotifications(); // Save notifications to localStorage
    displayNotifications(); // Refresh the display
});

// Load notifications when the page is loaded
window.onload = function() {
    loadNotifications(); // Load notifications from localStorage
    displayNotifications(); // Display loaded notifications
    connectWebSocket(); // Connect to WebSocket
};
// Ensure elements exist before adding event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Call other functions
    fetchOverviewData();
    populateEmployeeDropdown(); // Populate the employee dropdown
    fetchAdminFinanceRecords(); // Fetch and display all finance records
    fetchFinanceHistory(); // Fetch and display finance history
    fetchLeaveRequests(); // Fetch and display all leave requests
    fetchLeaveHistory(); // Fetch and display leave history
    getAllEmployees(); // Fetch and display all employees
    getAllContacts(); // Fetch and display all contacts
    getAllTasks(); // Fetch and display all tasks
    connectWebSocket(); // Connect to WebSocket for notifications
});