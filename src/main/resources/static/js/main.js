document.addEventListener("DOMContentLoaded", () => {
    const settingsButton = document.getElementById("settingsButton");
    const settingsMenu = document.getElementById("settingsMenu");
    const topBar = document.querySelector(".top-bar");

    settingsButton.addEventListener("click", () => {
        settingsMenu.classList.toggle("active");
        topBar.classList.toggle("active");
    });
});
