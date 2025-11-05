document.addEventListener("DOMContentLoaded", () => {
  const container = document.querySelector(".cards-container");
  if (!container) return;

  // Skeletons
  container.innerHTML = "";
  for (let i = 0; i < 6; i++) {
    const sk = document.createElement("div");
    sk.className = "card";
    sk.innerHTML = `<p>Loading...</p>`;
    container.appendChild(sk);
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8000); // 8s timeout

  fetch("/news?page=0&size=6", { signal: controller.signal })
    .then(async res => {
      clearTimeout(timer);

      // Guard: ensure JSON (sometimes server returns HTML error page)
      const ct = res.headers.get("content-type") || "";
      const text = await res.text();
      if (!res.ok) {
        console.error("HTTP error:", res.status, text);
        throw new Error("HTTP " + res.status);
      }
      if (!ct.includes("application/json")) {
        console.error("Not JSON:", ct, text.slice(0, 200));
        throw new Error("Server did not return JSON");
      }
      return JSON.parse(text);
    })
    .then(page => {
      const articles = page.content || [];
      container.innerHTML = "";
      if (articles.length === 0) {
        container.innerHTML = `<p style="opacity:.7">No news yet.</p>`;
        return;
      }
      for (const a of articles) {
        const card = document.createElement("div");
        card.className = "card";
        card.innerHTML = `
          <h3>${a.title || "Untitled"}</h3>
          <p><b>Source:</b> ${a.source || "Unknown"}</p>
          <p>${a.description || ""}</p>
          <a href="${a.url || "#"}" target="_blank">Read more</a>
        `;
        container.appendChild(card);
      }
    })
    .catch(err => {
      console.error("Fetch failed:", err);
      container.innerHTML = `<p style="color:#f66">Request failed or timed out.</p>`;
    });
});
