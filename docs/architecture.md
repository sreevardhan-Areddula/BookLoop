# Architecture

## Current direction

BookLoop will use a separate frontend and backend within this repository:

- `frontend/` will contain a React application built with Vite and TypeScript.
- `backend/` contains a Spring Boot 4.1.2 application built with Maven and targeting Java 21.
- MySQL is the selected database.

The frontend and backend are separate parts of the project. The React app calls the Spring API. Local API and email-verification connections are configured through environment variables.

The current system has Java 25 and Maven 3.9.16 available. The backend targets Java 21 bytecode and is built with Java 25; Spring Boot 4.1.2 supports Java versions through 27. The backend uses Spring MVC, Spring Data JPA, MySQL Connector/J, Spring Security Crypto, and Flyway. Account and email-verification tables are created from versioned migrations. Local development uses MySQL Community Server 8.4.13 LTS bound to `127.0.0.1`, a `bookloop_dev` database, and credentials in the ignored `backend/.env.local` file. `backend/run-local.ps1` starts MySQL when needed, loads the app credentials, and launches Spring Boot.

## Frontend setup

- Scaffold: Vite React TypeScript template.
- Styling: plain CSS for the initial placeholder; future page designs are deferred.
- Linting: Oxlint.
- The `App` component includes the email-verification page; the marketplace experience remains to be built.
- Frontend packages are installed from `frontend/package-lock.json`.

## Decisions deferred

The following decisions remain open and should be agreed before their implementation:

- Hosting and deployment strategy
- Authentication and authorization
- API structure and endpoint contracts
- Database schemas beyond accounts and email verification
- Product workflows and user interface

## Function documentation practice

Before adding a function to the frontend or backend, its intended behavior will be explained and agreed as part of the task. Documentation will be updated as architecture decisions are made.
