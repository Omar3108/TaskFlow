# TaskFlow — working agreement for Claude Code

TaskFlow is a collaborative kanban project manager (a simplified Jira/Trello). It is Omar's
portfolio project for remote Full Stack Java + React roles. Two goals of equal weight:
ship a professional, deployed, documented product, and **understand every decision** well
enough to defend it in technical interviews.

## Language

- Explanations to Omar: **Spanish**.
- Code, comments, commits, branch names, README: **English**. UI copy: English.

## How to work

- **Plan first.** Before each phase show the plan (files, key decisions) and wait for approval.
- **One phase at a time.** Stop at the end of each phase; do not continue without an OK.
- End-of-phase report, always these six items:
  1. Summary of what was done and why.
  2. The 2–3 most important technical decisions, with discarded alternatives.
  3. Files to review most carefully.
  4. How to test it (commands, endpoints, screens).
  5. **3 technical-interview questions** about the phase.
  6. Suggested commit message(s).
- Small commits, Conventional Commits (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`).
- One branch per phase (`feature/auth`, `feature/boards`, …). Claude commits **and pushes** the
  branch unless told otherwise; Omar opens and merges the pull request on GitHub (no `gh` CLI).
- If scope is unclear, **ask** instead of assuming.
- Never add a dependency without saying which one and why.
- If asked to explain something, explain it before writing more code.
- Every feature ships with its tests in the same phase.
- No secrets in code. Configuration via environment variables; keep `.env.example` current.

### Parts Omar writes himself

For these, provide only the structure with `// TODO` and an explanation of what each piece
must do. Then review his code like a pull request (bugs, security, best practices):

- Spring Security configuration and the JWT filter (`SecurityConfig`,
  `JwtAuthenticationFilter`, the token service).
- The logic that moves cards between columns and recalculates positions.
- Optimistic locking (`@Version`) for concurrent card edits, and its integration test.

## Stack

- **Backend:** Java 21, Spring Boot 3.5.x, Maven (wrapper). Spring Web, Data JPA, PostgreSQL,
  Flyway, Spring Security + JWT, Bean Validation, MapStruct, `@RestControllerAdvice` +
  `ProblemDetail`, springdoc-openapi, Actuator. Tests: JUnit 5, Mockito, MockMvc,
  Testcontainers, JaCoCo (target 70%+).
- **Frontend:** React 19 + TypeScript + Vite, React Router, TanStack Query, React Hook Form +
  Zod, Tailwind CSS 4, dnd-kit, ESLint + Prettier. Tests: Vitest + React Testing Library,
  Playwright (2–3 E2E flows).
- **Infra:** multi-stage Dockerfiles, `docker-compose.yml`, GitHub Actions, backend + PostgreSQL
  on Render/Railway, frontend on Vercel.

Environment notes:

- The local JDK is 25; the build targets Java 21 (`java.version`), and CI/Docker use Temurin 21.
- TypeScript is pinned to 6.x because `typescript-eslint` does not support TypeScript 7 yet.
- Spring Initializr no longer offers Boot 3.x, so `pom.xml` is maintained by hand. Migrating to
  Boot 4 is a documented "next step", not part of the current scope.

## Agreed design decisions

- **IDs:** UUID everywhere.
- **No Lombok:** `record` DTOs; JPA entities with explicit accessors. Entities are never exposed
  by the API (MapStruct mappers).
- **Auth:** short-lived access token kept in memory on the client; refresh token in an
  `HttpOnly; Secure; SameSite` cookie, rotated on every use and stored hashed (`refresh_token`
  table). In production Vercel rewrites `/api/*` to the backend so the cookie is same-site.
- **Invitations (MVP):** no e-mail. An ADMIN invites by e-mail address, the API returns a
  shareable token link, and invitees also see pending invitations after login. Tokens are
  stored hashed. Real e-mail arrives with the extras.
- **Ordering:** `position` is `DOUBLE PRECISION` (fractional indexing: a moved card gets the
  midpoint between its neighbours, so a move updates one row).
- **Optimistic locking:** `version` only on `Card`; sent in the request body; stale → HTTP 409.
- **Authorization:** resource → workspace → caller's role, resolved by one
  `WorkspaceAccessService` called explicitly from services. Non-members get **404**, not 403.
- **Roles:** exactly one OWNER per workspace (no ownership transfer in the MVP). MEMBER works
  with cards and comments; ADMIN also manages boards, columns, labels, members and invitations;
  OWNER also changes roles and renames/deletes the workspace.
- **Deletes:** hard delete with cascade. **Search:** per board, `ILIKE` on title/description.
  A card has a single assignee. Labels belong to a board.
- **Schema:** Flyway owns it (`ddl-auto: validate`), one migration set per phase. Enums are
  stored as strings. The `activity` table is deferred to the extras phase.
- **API:** prefix `/api/v1`; nested routes to create/list (`/boards/{id}/columns`), flat routes
  to operate on a resource (`/cards/{id}`); `PATCH /cards/{id}/move` is a dedicated endpoint
  where the client sends neighbour ids and the server computes the position; errors as
  `ProblemDetail`; pagination with `page`/`size`/`sort`.
- **Code layout:** package-by-feature in the backend (`auth`, `user`, `workspace`, `board`,
  `card`, `security`, `common`); feature folders in the frontend (`src/app`, `src/features/*`,
  `src/pages`, `src/components/ui`, `src/lib`, `src/types`, `src/test`).

## Commands

```bash
docker compose up -d                 # PostgreSQL on localhost:5432
cd backend && ./mvnw spring-boot:run # API on http://localhost:8080
cd backend && ./mvnw verify          # build + tests (needs Docker running) + JaCoCo report
cd frontend && npm run dev           # UI on http://localhost:5173
cd frontend && npm run lint && npm run format:check && npm run test && npm run build
```

Backend tests whose class name ends in `Test` are run by Surefire. Integration tests extend
`AbstractIntegrationTest` (shared PostgreSQL Testcontainer) and are named `*IntegrationTest`.

## Phase status

| # | Phase | Status |
|---|---|---|
| 0 | Setup: monorepo, Spring Boot, Vite, compose, Flyway, CI | Done — pending merge |
| 1 | Authentication (security code written by Omar) | Not started |
| 2 | Workspaces and members | Not started |
| 3 | Boards, columns and cards (move logic + optimistic locking by Omar) | Not started |
| 4 | Labels, comments, search and filters | Not started |
| 5 | Frontend polish | Not started |
| 6 | Testing (70%+ backend coverage, component tests, Playwright) | Not started |
| 7 | Docker, CI/CD and deployment, demo user | Not started |
| 8 | Extras: WebSockets, notifications, activity history, dashboard, attachments | Not started |
| 9 | README in English | Not started |

Update this table and the decisions above whenever something changes.
