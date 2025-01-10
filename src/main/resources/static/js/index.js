document.getElementById('loginForm').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // Reset error messages
    document.getElementById('error-message').style.display = 'none';
    document.getElementById('emailError').style.display = 'none';
    document.getElementById('passwordError').style.display = 'none';

    // Validate email format
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
        document.getElementById('emailError').style.display = 'block';
        return;
    }

    // Validate password format
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}$/; // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
    if (!passwordPattern.test(password)) {
        document.getElementById('passwordError').style.display = 'block';
        return;
    }

    console.log('Attempting to log in with:', { email, password });

    // Send a POST request to the login endpoint
    fetch('/api/employees/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email: email,
            password: password
        })
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    console.error('Invalid credentials');
                    throw new Error('Invalid credentials');
                } else {
                    console.error('Login failed with status:', response.status);
                    throw new Error ('Login failed');
                }
            }
            return response.json();
        })
        .then(data => {
            console.log('Login response data:', data);
            localStorage.setItem('jwt', data.jwt);

            const tokenPayload = JSON.parse(atob(data.jwt.split('.')[1]));
            const role = tokenPayload.role;
            console.log('User  role:', role);

            // Check for the role with the "ROLE_" prefix
            if (role === 'ROLE_ADMIN') {
                window.location.href = '/admin-dashboard';
            } else if (role === 'ROLE_EMPLOYEE') {
                window.location.href = '/employee-dashboard';
            } else {
                alert('Unknown role. Please contact support.');
            }
        })
        .catch(error => {
            console.error('Error during login:', error);
            document.getElementById('error-message').innerText = 'Login failed. Please check your credentials and try again.';
            document.getElementById('error-message').style.display = 'block';
        });

});

// Password visibility toggle
document.getElementById('togglePassword').addEventListener('click', function() {
    const passwordInput = document.getElementById('password');
    const toggleButton = document.getElementById('togglePassword');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleButton.innerText = 'Hide';
    } else {
        passwordInput.type = 'password';
        toggleButton.innerText = 'Show';
    }
});