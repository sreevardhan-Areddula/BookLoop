# Frontend

BookLoop's frontend uses React, Vite, and TypeScript. Plain CSS is used for the initial placeholder screen; page design and routing will be decided feature by feature.

## Requirements

- Node.js 22.14.0 or a compatible Node.js 22 release
- npm 10 or a compatible npm release

## Run locally

From this directory, install dependencies with `npm install`, then start the development server with `npm run dev`.

## Available commands

- `npm run dev` — start the Vite development server.
- `npm run build` — type-check and create a production build.
- `npm run lint` — check source files with Oxlint.
- `npm run preview` — serve the production build locally.

## Initial component

`src/App.tsx` contains the `App` component. It renders a static BookLoop welcome placeholder and has no marketplace behavior or API calls.
