# BookLoop frontend

BookLoop's frontend uses React, TypeScript, Vite, and plain CSS. The single-page app provides account registration, email verification, sign-in, profile management, book browsing and listing, exchange workflows, and Sale interest requests. It calls the Spring Boot API at `http://localhost:8081` by default.

## Requirements

- Node.js 22.14.0 or a compatible Node.js 22 release
- npm 10 or a compatible npm release

## Run locally

From this directory, install dependencies with `npm install`, then start the development server with `npm run dev`. Vite serves the app at `http://localhost:5173` by default. Routes such as `/books`, `/profile`, and `/exchanges` are handled by the SPA; they should load through the Vite development server.

Set `VITE_API_BASE_URL` when the backend uses a different origin.

## Available commands

- `npm run dev` — start Vite.
- `npm run build` — type-check and create a production build.
- `npm run lint` — check source files with Oxlint.
- `npm run preview` — serve the production build locally.

Feature behavior and the split between implemented and pending flows are listed in [implementation status](../docs/implementation-status.md).
