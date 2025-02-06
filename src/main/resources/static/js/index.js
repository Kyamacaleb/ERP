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
        document.getElementById('emailError').innerText = 'Incorrect email format.';
        document.getElementById('emailError').style.display = 'block';
        return;
    }

    // Validate password format
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$/; // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special character
    if (!passwordPattern.test(password)) {
        document.getElementById('passwordError').innerText = 'Password must be at least 8 characters long and include uppercase letter, lowercase letter, special character, and number.';
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
                return response.json().then(errorData => {
                    // Check if errorData is defined and has an error property
                    if (errorData && errorData.error) {
                        if (errorData.error === 'Incorrect email format.') {
                            document.getElementById('emailError').innerText = 'Incorrect email format. Please enter a valid email format.';
                            document.getElementById('emailError').style.display = 'block';
                        } else if (errorData.error === 'Incorrect email.') {
                            document.getElementById('emailError').innerText = 'Incorrect email. Please check your email.';
                            document.getElementById('emailError').style.display = 'block';
                        } else if (errorData.error === 'Incorrect password.') {
                            document.getElementById('passwordError').innerText = 'Incorrect password. Please enter the correct password.';
                            document.getElementById('passwordError').style.display = 'block';
                        } else {
                            document.getElementById('error-message').innerText = 'Wrong credentials used. Please check your credentials.';
                            document.getElementById('error-message').style.display = 'block';
                        }
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            // Handle successful login
            console.log('Login response data:', data);
            localStorage.setItem('jwt', data.jwt);

            const tokenPayload = JSON.parse(atob(data.jwt.split('.')[1]));
            const role = tokenPayload.role;
            console.log('User  role:', role);

            // Redirect based on role
            if (role === 'ROLE_ADMIN') {
                window.location.href = '/admin-dashboard';
            } else if (role === 'ROLE_EMPLOYEE') {
                window.location.href = '/employee-dashboard';
            } else {
                alert('Unknown role. Please contact support.');
            }
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