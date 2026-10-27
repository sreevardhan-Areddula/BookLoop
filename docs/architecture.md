# BookLoop architecture

## Current system

BookLoop is a local, single-page React application backed by a Spring Boot REST API and MySQL. The frontend and backend live in separate folders in this repository.

- `frontend/`: React, TypeScript, Vite, CSS, and Oxlint. The app handles browser navigation for `/books`, `/profile`, and `/exchanges`.
- `backend/`: Spring Boot 4.1.2, Maven, Spring MVC, Spring Security, Spring Data JPA, Flyway, and MySQL Connector/J. The source targets Java 21. The local machine currently runs Java 25.
- Database: MySQL Community Server 8.4 LTS. Versioned Flyway migrations create and evolve the schema.
- Local scripts: `backend/setup-local-mysql.ps1` initializes the local database; `backend/run-local.ps1` starts MySQL if needed, loads ignored local environment settings, and launches the API.

The local frontend runs at `http://localhost:5173`; the API listens at `http://localhost:8081`. The browser sends API requests to the configured API origin. Gmail delivery is configured through environment variables and is not committed to source control.

## Authentication and data boundaries

Members authenticate with email and password. Passwords are stored as Argon2id hashes. Short-lived JWT access tokens are kept in browser memory; rotating refresh tokens are held in `HttpOnly` cookies, with CSRF protection on refresh and logout. Email verification is required before member access. A public city is required before members can use the marketplace.

MySQL stores account and marketplace records, including private sale-bill bytes. Sale bills are not exposed as public URLs. English OCR runs locally in the backend and only grants a tick when it reads a paid total greater than the asking price; it does not authenticate the bill.

## Implemented and pending features

See [implementation status](implementation-status.md) for the detailed current feature boundary. Exchange chat is implemented. Sale interest requests and seller acceptance/decline are implemented, but the Sale-specific private chat, redacted bill sharing, and sold-marking flow remain pending. Free-book claiming is also pending.

## Deployment

Hosting and production deployment have not been selected. Local configuration and OCR runtime needs are documented in [backend setup](../backend/README.md) and [Sale bill OCR](sale-bill-ocr.md).

## Documentation practice

Product decisions live in [product requirements](product-requirements.md); current behavior and unfinished workflows live in [implementation status](implementation-status.md) and the feature guides. When a workflow or API changes, update its guide and the status page in the same change. Keep local credentials in ignored environment files.
