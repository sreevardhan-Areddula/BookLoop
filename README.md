# BookLoop

BookLoop is a peer-to-peer book-sharing and marketplace platform.

## Project structure

- `frontend/` — React application built with Vite and TypeScript.
- `backend/` — Spring Boot application built with Maven and Java 21.
- `docs/` — Project architecture and technical decisions.

## Selected stack

- Frontend: React, Vite, TypeScript
- Backend: Spring Boot, Maven, Java 21
- Database: MySQL Community Server 8.4 LTS

Hosting and deployment choices will be made in a later phase. Application features, API contracts, authentication, and database schema have not been defined yet.

## Current status

The frontend scaffold and initial Spring Boot backend scaffold are in place. Marketplace features, API contracts, authentication, database schema, and hosting have not been defined yet.
# BookLoop

BookLoop is a peer-to-peer book marketplace and exchange platform built with React, TypeScript, Spring Boot, and MySQL.

## Local setup

- Frontend: see [frontend/README.md](frontend/README.md)
- Backend, MySQL, and Gmail configuration: see [backend/README.md](backend/README.md)
- Authentication flows and local secrets: see [docs/authentication.md](docs/authentication.md)
- Email verification setup: see [docs/email-verification.md](docs/email-verification.md)

Authentication currently includes signup, email verification, Argon2id password storage, JWT access tokens, rotating HttpOnly refresh cookies, city completion, logout, and password recovery.
