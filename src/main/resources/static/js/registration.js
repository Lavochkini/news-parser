document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("registrationForm");
    const usernameInput = document.querySelector("input[name='username']");
    const emailInput = document.querySelector("input[name='email']");
    const passwordInput = document.querySelector("input[name='password']");
    const confirmPasswordInput = document.querySelector("input[name='confirmPassword']");

    const usernameError = document.getElementById("usernameError");
    const emailError = document.getElementById("emailError");
    const passwordError = document.getElementById("passwordError");
    const confirmPasswordError = document.getElementById("confirmPasswordError");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        // Очистка попередніх помилок
        usernameError.textContent = "";
        emailError.textContent = "";
        passwordError.textContent = "";
        confirmPasswordError.textContent = "";

        const username = usernameInput.value.trim();
        const email = emailInput.value.trim();
        const password = passwordInput.value.trim();
        const confirmPassword = confirmPasswordInput.value.trim();

        let hasError = false;

        // Перевірка username
        if (!username) {
            usernameError.textContent = "Username is required";
            hasError = true;
        }

        // Перевірка email
        if (!email) {
            emailError.textContent = "E-mail is required";
            hasError = true;
        } else if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(email)) {
            emailError.textContent = "Incorrect E-mail format";
            hasError = true;
        }

        // Перевірка password
        if (!password) {
            passwordError.textContent = "Password is required";
            hasError = true;
        } else if (password.length < 6) {
            passwordError.textContent = "Password must be at least 6 characters";
            hasError = true;
        }

        // Перевірка confirmPassword
        if (password !== confirmPassword) {
            confirmPasswordError.textContent = "Passwords do not match";
            hasError = true;
        }

        if (hasError) return;

        try {
            const response = await fetch("/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, email, password })
            });

            const result = await response.json();

            if (response.ok && result.status === "success") {
                window.location.href = "/login";
            } else {
                confirmPasswordError.textContent = result.message || "Registration failed";
            }
        } catch (err) {
            console.error(err);
            confirmPasswordError.textContent = "Server error. Try again later.";
        }
    });
});
