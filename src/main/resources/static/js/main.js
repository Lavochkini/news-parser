document.addEventListener("DOMContentLoaded", () => {
    const btn   = document.getElementById("settingsButton");
    const panel = document.getElementById("settingsPanel");

    if (btn && panel) {
        btn.addEventListener("click", () => {
            panel.classList.toggle("active");
        });
    }

    // Масив статей (можна змінити або підвантажувати з API)
    const articles = [
        {
            title: "Spring Boot Tutorial",
            author: "John Doe",
            description: "Learn how to build apps with Spring Boot step by step.",
            url: "https://spring.io/projects/spring-boot"
        },
        {
            title: "Java 21 Released",
            author: "Jane Smith",
            description: "New features in Java 21 and how to use them effectively.",
            url: "https://openjdk.org/"
        },
        {
            title: "Thymeleaf Guide",
            author: "Alex Johnson",
            description: "Building dynamic UIs with Thymeleaf templates.",
            url: "https://www.thymeleaf.org/"
        },
        {
            title: "Frontend Development",
            author: "Maria Wilson",
            description: "Best practices for modern frontend applications.",
            url: "https://developer.mozilla.org/"
        }
    ];

    // Підставляємо дані у картки
    const cards = document.querySelectorAll(".card");
    cards.forEach((card, index) => {
        if (articles[index]) {
            card.innerHTML = `
                <h3>${articles[index].title}</h3>
                <p><b>Author:</b> ${articles[index].author}</p>
                <p>${articles[index].description}</p>
                <a href="${articles[index].url}" target="_blank">Read more</a>
            `;
        }
    });
});
