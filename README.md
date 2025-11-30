# Phish-Hawk V2 🦅

This is the main web application for Phish-Hawk, a tool to analyze emails for phishing threats. This is V2 of the project, which upgrades the original rule-based system with a powerful AI backend.

Users can upload an email file (`.eml`, `.pdf`, `.txt`) or paste the raw email source to get an instant analysis.

---

### V2 Architecture: The AI Upgrade

The biggest change in V2 is the move to a **microservice architecture**.

-   **V1:** All logic was in Java. It used a simple list of keywords to check for spam. This was fast but not very accurate. (https://github.com/loch214/Phish-Hawk.git)
-   **V2:** The Java backend now calls a separate **Python AI service** (via a REST API) to analyze the email's content. This makes the detection much smarter and more accurate. The old rule-based checks for technical issues (like header spoofing) are still used alongside the AI for a more complete analysis.

This new architecture separates the web application logic (Java) from the machine learning logic (Python), which is a common practice in modern software engineering.

---

### Tech Stack

-   **Java 17**
-   **Spring Boot 3**
-   **Spring WebFlux (`WebClient`):** To make HTTP calls to the Python microservice.
-   **Maven:** For project management.
-   **Frontend:** HTML, CSS, and Vanilla JavaScript.

---

### How to Run It

1.  Make sure the [Phish-Hawk AI Service](https://github.com/your-username/phish-hawk-ai) is running first at `http://localhost:5000`.
2.  Open the project in IntelliJ IDEA.
3.  Run the `PhishHawkV2Application.java` file.
4.  The web application will be available at `http://localhost:8080`.
