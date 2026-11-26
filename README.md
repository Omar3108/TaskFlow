# TaskFlow

[![CI](https://github.com/Omar3108/TaskFlow/actions/workflows/ci.yml/badge.svg)](https://github.com/Omar3108/TaskFlow/actions/workflows/ci.yml)

Collaborative kanban project manager: workspaces, boards, columns and cards with
drag and drop, roles and per-resource authorization.

> Work in progress. Full documentation (live demo, screenshots, architecture and technical
> decisions) will be added when the MVP is deployed.

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway
- **Frontend:** React, TypeScript, Vite, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS
- **Tooling:** Docker Compose, GitHub Actions, Testcontainers, Vitest

## Running locally

Requirements: JDK 21+, Node.js 26, Docker.

```bash
cp .env.example .env          # required: local settings and the JWT signing key
docker compose up -d          # PostgreSQL

cd backend
./mvnw spring-boot:run        # API on http://localhost:8080 (docs: /swagger-ui)

cd ../frontend
npm install
npm run dev                   # UI on http://localhost:5173
```

### Whole app in Docker

To try the app without installing Java or Node, run everything in containers:

```bash
cp .env.example .env                          # if you have not already
docker compose --profile app up -d --build    # PostgreSQL + API + web
```

Open http://localhost:3000 (change it with `WEB_PORT` in `.env`). nginx serves the frontend and
forwards `/api` to the API container, which is not published on the host.
`docker compose --profile app down` stops it; the database volume is kept.

## Deployment

Frontend on Vercel, API on Render, PostgreSQL on Neon, deployed from GitHub Actions after the
tests pass. Step by step: [`docs/deployment.md`](docs/deployment.md).

## Tests

```bash
cd backend && ./mvnw verify            # unit + integration tests (needs Docker); fails below 80% lines / 70% branches
cd frontend && npm run test:coverage   # component tests with coverage minimums

# End-to-end: three user flows in a real browser against the app in Docker
docker compose --profile app up -d --build --wait
cd e2e && npm ci && npx playwright install chromium && npm test
```

Coverage reports: `backend/target/site/jacoco/index.html` and `frontend/coverage/index.html`.
See [`e2e/README.md`](e2e/README.md) for the end-to-end flows.
