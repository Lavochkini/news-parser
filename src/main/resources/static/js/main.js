document.addEventListener("DOMContentLoaded", () => {
    const settingsButton = document.getElementById("settingsButton");
    const settingsMenu = document.getElementById("settingsMenu");

    settingsButton.addEventListener("click", () => {
        settingsMenu.classList.toggle("hidden");
    });
});
