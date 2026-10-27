# Backend

BookLoop's backend is a Spring Boot 4.1.2 application built with Maven. It targets Java 21 and uses Spring MVC, Spring Data JPA, MySQL, Flyway, Spring Security, and JWT resource-server support. The current account APIs include registration, email verification, login, refresh-token rotation, profile city completion, logout, and password recovery. See [the authentication guide](../docs/authentication.md) for the flows and configuration.

## Requirements

- JDK 21 or newer. The project compiles for Java 21; Java 25 is currently installed on this system.
- Maven 3.6.3 or newer. Maven 3.9.16 is installed on this system.

## Run locally

Run `./setup-local-mysql.ps1` once from this directory to initialize MySQL Community Server 8.4.13 LTS and create the local `bookloop_dev` database. The script binds MySQL to localhost, generates local credentials, and stores the app credentials in the ignored `.env.local` file. Keep the MySQL server folder and `.env.local` file on this machine. The optional `-Port` parameter supports a temporary port when another MySQL instance is already using 3306.

Start the backend with `./run-local.ps1`. It starts MySQL if needed, reads `.env.local` for the current process, and starts Spring Boot.

The API listens on `http://localhost:8081` by default. Opening the base URL returns a small JSON status response; application features are under `/api`. Flyway applies database migrations at startup. Email delivery requires the Gmail variables documented in `../docs/email-verification.md`. JWT signing also requires a locally generated `BOOKLOOP_JWT_SECRET` in `.env.local`; the setup process on this machine created one.

Book listing and discovery behavior is documented in `../docs/book-listings.md`.

Build the application with `mvn package`.

The local MySQL process runs in the background on `127.0.0.1:3306`. Its data directory is `C:\Users\<your-user>\mysql-8.4.13-winx64\data-8.4`. The app database account is limited to `bookloop_dev`; the MySQL root password is randomly reset during setup and is not used by the app.

## Application entry point

`src/main/java/com/bookloop/BookLoopApplication.java` contains the `main` method. It starts the Spring Boot application; it does not define an API endpoint or marketplace behavior.
