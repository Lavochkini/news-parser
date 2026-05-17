document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("loginForm");
    const emailInput = document.querySelector("input[name='email']");
    const passwordInput = document.querySelector("input[name='password']");
    const emailError = document.getElementById("emailError");
    const passwordError = document.getElementById("passwordError");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        emailError.textContent = "";
        passwordError.textContent = "";

        const email = emailInput.value.trim();
        const password = passwordInput.value.trim();

        // Простенька валідація на фронті
        let hasError = false;
        if (!email) {
            emailError.textContent = "Write your E-mail";
            hasError = true;
        } else if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(email)) {
            emailError.textContent = "Incorrect E-mail format";
            hasError = true;
        }
        if (!password) {
            passwordError.textContent = "Password is required";
            hasError = true;
        } else if (password.length < 6) {
        passwordError.textContent = "Password must be at least 6 characters";
        hasError = true;
    }
        if (hasError) return;

        try {
            const response = await fetch("/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            const result = await response.json();

            if (response.ok && result.token) {
                Auth.save(result);
                window.location.href = "/main";
            } else {
                passwordError.textContent = result.message || "Invalid email or password";
            }
        } catch (err) {
            console.error(err);
            passwordError.textContent = "Server error. Try again later.";
        }
    });
});
