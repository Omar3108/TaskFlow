# TaskFlow

[![CI](https://github.com/Omar3108/TaskFlow/actions/workflows/ci.yml/badge.svg)](https://github.com/Omar3108/TaskFlow/actions/workflows/ci.yml)

Collaborative kanban project manager: workspaces, boards, columns and cards with
drag and drop, roles and per-resource authorization.

> Work in progress. Full documentation (live demo, screenshots, architecture and technical
> decisions) will be added when the MVP is deployed.

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Flyway
- **Frontend:** React, TypeScript, Vite, Tailwind CSS
- **Tooling:** Docker Compose, GitHub Actions, Testcontainers, Vitest

## Running locally

Requirements: JDK 21+, Node.js 26, Docker.

```bash
cp .env.example .env          # optional: the defaults work for local development
docker compose up -d          # PostgreSQL

cd backend
./mvnw spring-boot:run        # API on http://localhost:8080 (health: /actuator/health)

cd ../frontend
npm install
npm run dev                   # UI on http://localhost:5173
```

## Tests

```bash
cd backend && ./mvnw verify   # needs Docker running (Testcontainers)
cd frontend && npm run test
```
