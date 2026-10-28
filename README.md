# BookLoop

BookLoop is a peer-to-peer book-sharing, exchange, and sale-interest platform.

## Project structure

- `frontend/` — React, TypeScript, and Vite application.
- `backend/` — Spring Boot REST API built with Maven.
- `docs/` — Product decisions, workflows, API details, and setup guides.

## Stack

- Frontend: React, TypeScript, Vite, CSS, and Oxlint.
- Backend: Spring Boot, Spring Security, Spring Data JPA, and Flyway.
- Database: MySQL Community Server 8.4 LTS.
- Backend source targets Java 21; local development has also been run on Java 25.

## Current features

BookLoop currently supports verified member accounts, profile management, book listings, search, exchange requests and chat, OCR-assisted sale-bill price checks, Sale interest/chat/sold workflows, and owner-reviewed Free-book claims with private chat and handover completion. See [implementation status](docs/implementation-status.md) for the current feature boundary and [product requirements](docs/product-requirements.md) for approved behavior.

## Run locally

- Frontend: [frontend/README.md](frontend/README.md)
- Backend and MySQL: [backend/README.md](backend/README.md)
- Authentication and local secret configuration: [authentication guide](docs/authentication.md)
- System design: [architecture](docs/architecture.md)

Hosting and deployment choices are deferred until the local application is ready for that phase.
