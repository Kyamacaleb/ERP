document.addEventListener('DOMContentLoaded', function() {
    console.log('Employee dashboard script loaded');

    // Utility function to get authentication headers
    function getAuthHeaders() {
        const token = localStorage.getItem('jwt'); // Retrieve the JWT from local storage
        console.log('JWT:', token); // Log the JWT to see if it's retrieved correctly
        const decodedToken = jwt_decode(token); // Decode JWT to extract payload
        const employeeId = decodedToken.employeeId; // Store employeeId globally

        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`, // Include the JWT in the Authorization header
            'X-Employee-Id': employeeId // Include employeeId as a custom header
        };
    }

    // Function to load personal information
    async function loadPersonalInfo() {
        const response = await fetch('/api/employees/me', {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const employee = await response.json();
            document.getElementById('fullName').textContent = `${employee.firstName} ${employee.lastName}`;
            document.getElementById('email').textContent = employee.email;
            document.getElementById('phone').textContent = employee.phoneNumber;
            document.getElementById('department').textContent = employee.department;
            document.getElementById('dateOfJoining').textContent = employee.dateOfJoining;
            document.getElementById('emergencyContactName').textContent = employee.emergencyContactName;
            document.getElementById('emergencyContactPhone').textContent = employee.emergencyContactPhone;
            document.getElementById('profilePicture').src = employee.profilePicture ? `/api/employees/${employee.employeeId}/profile-picture` : 'default-profile.jpg'; // Fallback to a default image if none is provided
        } else {
            console.error('Failed to load personal information:', response.status);
        }
    }

    // Handle profile picture upload
    document.getElementById('profilePictureUpload').addEventListener('change', function(event) {
        const file = event.target.files[0];
        if (file) {
            const formData = new FormData();
            formData.append('file', file);

            // Use the employee ID from the decoded JWT
            const employeeId = jwt_decode(localStorage.getItem('jwt')).employeeId;

            fetch(`/api/employees/${employeeId}/upload-profile-picture`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: formData,
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to upload profile picture');
                    }
                    return response.text();
                })
                .then(message => {
                    alert(message);
                    loadPersonalInfo(); // Refresh the personal information displayed
                })
                .catch(error => console.error('Error uploading profile picture:', error));
        }
    });

    // Handle profile update
    document.getElementById('editProfileForm').addEventListener('submit', function(event) {
        event.preventDefault(); // Prevent the default form submission

        const updatedEmployee = {
            firstName: document.getElementById('editFirstName').value,
            lastName: document.getElementById('editLastName').value,
            phoneNumber: document.getElementById('editPhone').value,
            department: document.getElementById('editDepartment').value,
            emergencyContactName: document.getElementById('editEmergencyContactName').value,
            emergencyContactPhone: document .getElementById('editEmergencyContactPhone').value,
        };

        fetch('/api/employees/me', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify(updatedEmployee),
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to update employee data');
                }
                return response.json();
            })
            .then(data => {
                alert('Profile updated successfully!');
                loadPersonalInfo(); // Refresh the personal information displayed
            })
            .catch(error => console.error('Error updating employee data:', error));
    });

    // Handle change password
    document.getElementById('changePasswordForm').addEventListener('submit', function(event) {
        event.preventDefault(); // Prevent the default form submission

        const newPassword = document.getElementById('newPassword').value;
        const currentPassword = document.getElementById('currentPassword').value;

        // Use the employee ID from the decoded JWT
        const employeeId = jwt_decode(localStorage.getItem('jwt')).employeeId;

        fetch(`/api/employees/me/change-password/${employeeId}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify({ currentPassword, newPassword })
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to change password');
                }
                return response.json();
            })
            .then(data => {
                alert('Password changed successfully!');
            })
            .catch(error => {
                console.error('Error changing password:', error);
                alert('Error changing password: ' + error.message);
            });
    });

    // Load initial data
    loadPersonalInfo();
    loadProfilePicture();
});