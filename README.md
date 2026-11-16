# Campus Share — Java + HTML + CSS edition

This edition keeps the existing Spring Boot Java domain model, services, repositories, pricing strategies, reservation logic, persistence and tests. The browser interface has been converted to server-rendered HTML pages styled with CSS using Thymeleaf. The Vite/React/TypeScript application and browser JavaScript frontend are not included in this package.

## Technology
- Java 17
- Spring Boot 3.3.5 (Maven)
- Thymeleaf for Java-rendered HTML
- HTML5 and CSS3
- Spring Data JPA / Hibernate
- H2 for local development; PostgreSQL profile for deployment
- No React, Vite, TypeScript, Python or client-side JavaScript is required to run this edition.

## Run on Windows
1. Install JDK 17 and Maven.
2. Open PowerShell in this folder (the folder containing `pom.xml`).
3. Run:

   ```powershell
   mvn clean spring-boot:run
   ```

4. Open http://localhost:8080

Keep the terminal open while using the website.

## Main pages
- `/home` — landing page
- `/register` — create student/faculty/staff account
- `/login` — sign in
- `/browse` — search campus resources; each resource card opens its detail page
- `/resources/{id}` — view a resource, see its units and request a reservation when signed in
- `/dashboard` — member dashboard and own listings
- `/listing/new` — publish a resource

The forms submit regular HTTP requests. Java controllers call the existing services; Thymeleaf renders the resulting HTML on the server. CSS is served from `src/main/resources/static/css/app.css`.

## Database
The default `dev` profile uses the existing H2 development configuration. PostgreSQL deployment configuration remains in `application-postgres.yml`. Review credentials and environment variables before any public deployment.

## Notes
- Existing REST API controllers are retained for backend compatibility, but the included website does not call them from browser JavaScript.
- Reservation requests are exposed through the Java-rendered resource detail page. The reservation owner still approves or rejects requests through the retained service/API workflows; additional owner/admin screens can be added as separate Java MVC controllers and HTML templates.
- Navigation changes based on the signed-in session. New accounts are automatically signed in after successful registration. The Java MVC login and API authentication share the same HTTP-session security context repository.
- JavaScript-free UI means dynamic client-side interactions are intentionally not used. Server-side validation/errors should be improved and tested before production use.
- Run tests with `mvn test`.
