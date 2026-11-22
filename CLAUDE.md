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

### Parts originally reserved for Omar

Three parts were meant to be written by Omar with Claude only reviewing. All three were
delegated, so the code is not his and he needs the reasoning explained to defend it:

- Spring Security and the JWT filter: written by Claude at his request (phase 1, 2026-11-15).
- The card move logic with position recalculation, and optimistic locking (`@Version`) with
  its integration test: written by a Sonnet subagent at his request (phase 3b, 2026-11-17),
  reviewed by Claude.

Standing instruction (2026-11-17): in every phase, anything that would have been reserved for
Omar is written by a Sonnet subagent that makes its own design decisions; Claude reviews the
result, reruns the tests and explains the decisions in the end-of-phase report. Omar merges the
pull requests and checks that nothing is broken.

## Stack

- **Backend:** Java 21, Spring Boot 3.5.x, Maven (wrapper). Spring Web, Data JPA, PostgreSQL,
  Flyway, Spring Security + JWT, Bean Validation, MapStruct, `@RestControllerAdvice` +
  `ProblemDetail`, springdoc-openapi, Actuator. Tests: JUnit 5, Mockito, MockMvc,
  Testcontainers, JaCoCo (target 70%+).
- **Frontend:** React 19 + TypeScript + Vite, React Router, TanStack Query, React Hook Form +
  Zod, Tailwind CSS 4, dnd-kit, sonner (toasts), ESLint + Prettier. Tests: Vitest + React Testing Library,
  Playwright (2–3 E2E flows).
- **Infra:** multi-stage Dockerfiles, `docker-compose.yml`, GitHub Actions, backend + PostgreSQL
  on Render/Railway, frontend on Vercel.

Environment notes:

- The local JDK is 25; the build targets Java 21 (`java.version`), and CI/Docker use Temurin 21.
- TypeScript is pinned to 6.x because `typescript-eslint` does not support TypeScript 7 yet.
- `frontend/package.json` overrides `minimatch` to `^11.0.8` inside
  `@typescript-eslint/typescript-estree` (GHSA-g3hv-75wg-m249). Remove the override once
  `typescript-eslint` depends on a patched version.
- The repository `.env` is read by docker-compose, by the backend (`spring.config.import`) and
  by `vite.config.ts`. On Omar's machine port 8080 is used by another project, so his local
  `.env` sets `PORT=8090` and `VITE_API_PROXY_TARGET=http://localhost:8090`.
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
- **Optimistic locking:** `version` only on `Card`; sent in the request body; stale → HTTP 409
  (checked by hand in `CardService.update`, and by Hibernate at flush for the remaining race).
  A move does **not** bump the version: `column`, `position` and `updatedAt` are
  `@OptimisticLock(excluded = true)` and `Card` is `@DynamicUpdate`, so an edit never writes a
  stale position back. Concurrent moves of one card are last-write-wins.
- **Moves:** `PositionCalculator.between` uses the midpoint, `next - STEP` at the start and
  `previous + STEP` at the end. When the gap between the neighbours is exhausted (below `1e-6`,
  or the midpoint collides), the whole list is rebalanced to multiples of 1024 with one native
  `UPDATE ... row_number()` statement, which bypasses `@Version`. A drop that changes nothing
  writes nothing. Invalid targets or neighbours answer 400 with a generic message.
- **Authorization:** resource → workspace → caller's role, resolved by one
  `WorkspaceAccessService` called explicitly from services. Non-members get **404**, not 403.
- **Roles:** exactly one OWNER per workspace (no ownership transfer in the MVP). MEMBER works
  with cards and comments; ADMIN also manages boards, columns, labels, members and invitations;
  OWNER also changes roles and renames/deletes the workspace.
- **Member rules:** only the OWNER changes roles (between ADMIN and MEMBER). Removing someone
  requires strictly outranking them, so an ADMIN cannot remove another ADMIN. Anyone but the
  OWNER can leave. A partial unique index guarantees one OWNER per workspace.
- **Invitation rules:** ADMIN+ can invite; only the OWNER can invite as ADMIN. Statuses:
  PENDING, ACCEPTED, DECLINED, REVOKED, EXPIRED; expiry (7 days) is evaluated on read, with no
  scheduled job. Accepting requires being signed in with the invited e-mail: a valid link used
  by another account gets 403, anything else invalid gets 404. Invitees can also accept or
  decline from `/users/me/invitations`.
- **Deletes:** hard delete with cascade. **Search:** per board, `ILIKE` on title/description.
  A card has a single assignee. Labels belong to a board.
- **Auth internals:** the JWT filter builds the principal (`AuthenticatedUser`: id + e-mail)
  from token claims with no database query; there are no global authorities. Refresh tokens are
  opaque random values (not JWTs), single-use, with reuse detection that revokes every session
  of the user. Login always runs BCrypt and returns one generic error.
- **Same-origin API:** the frontend calls relative `/api/v1` URLs. Vite proxies `/api` in
  development; the production host must rewrite it. Browsers still send an `Origin` header on
  proxied POSTs, so the frontend origin must be listed in `CORS_ALLOWED_ORIGINS` (remember this
  when deploying).
- **Naming:** tables are plural snake_case (`users`, `refresh_tokens`). Entity timestamps are
  set in `@PrePersist`/`@PreUpdate` so they exist before the first flush.
- **Schema:** Flyway owns it (`ddl-auto: validate`), one migration set per phase. Enums are
  stored as strings. The `activity` table is deferred to the extras phase.
- **API:** prefix `/api/v1`; nested routes to create/list (`/boards/{id}/columns`), flat routes
  to operate on a resource (`/cards/{id}`); `PATCH /cards/{id}/move` is a dedicated endpoint
  where the client sends neighbour ids and the server computes the position; errors as
  `ProblemDetail`; pagination with `page`/`size`/`sort`.
- **Boards, columns, cards:** boards and columns are managed by ADMIN+; every MEMBER works with
  cards. `GET /boards/{id}` returns the whole board (columns and card summaries, in order) in a
  fixed number of queries, plus the caller's role. Table `board_columns`, entity `BoardColumn`.
  New items are appended at `max(position) + 1024`; `PositionCalculator` (`common.ordering`)
  owns the position maths for both columns and cards.
- **New boards** start with "To do", "In progress" and "Done" unless `defaultColumns: false`
  (`CreateBoardRequest`; backend tests create empty boards through `BoardTestSupport`).
- **Board UI (drag and drop):** dnd-kit (`core`, `sortable`, `utilities`). The placement logic
  lives in pure functions (`features/boards/dnd.ts`): arrange by index, describe by neighbours
  (`cardMoveFor`), apply neighbours (`applyCardMove`, also used for the optimistic update).
  While dragging, `KanbanBoard` keeps a draft tied to the cached board object it came from, so
  it is ignored as soon as the cache changes (no flash between drop and optimistic update).
  Moves are optimistic (`optimisticBoardUpdate`: cancel, snapshot, write, rollback, refetch).
  Space picks up a card with the keyboard, Enter opens it. `BoardPage` is lazy-loaded so
  dnd-kit stays out of the main bundle.
- **Card dialog:** loads the full card (`refetchOnWindowFocus: false`, so typing is never
  reset); a 409 shows a notice with "Load latest version", and the form is keyed by `version`.
- **Card fields:** title, description, single assignee, due date (no priority; labels arrive in
  phase 4). Updates (`PATCH`) send every editable field: a missing or null field is cleared.
  The assignee must be a workspace member (400 otherwise); when a member is removed or leaves,
  `MemberRemovedEvent` unassigns their cards in the same transaction.
- **Labels:** belong to a board; ADMIN+ manages them, every member sees them and labels cards.
  Colours come from a fixed palette (`LabelColor`, 8 values, `CHECK` constraint). Names are
  unique per board ignoring case (expression index on `lower(name)`); lists sort by
  `lower(name)`. A card's labels travel in `PATCH /cards/{id}` as `labelIds` (missing = none),
  so they are version-checked and bump the version; ids from another board → 400.
- **Comments:** `GET /cards/{id}/comments` is paginated (`PageResponse`, newest first, only
  `createdAt` sortable, `max-page-size: 100`). Every member posts; only the author edits; the
  author or an ADMIN+ deletes (moderation). `author_id` is `ON DELETE SET NULL`.
- **Board filters:** `GET /boards/{id}` takes optional `q` (case-insensitive `LOWER(...) LIKE`
  on title and description, wildcards escaped), `assigneeId`, `unassigned=true`, `labelId`,
  `due=OVERDUE|WEEK|NONE` (UTC), combined with AND via JPA Specifications
  (`CardSpecifications`). All columns are returned, only matching cards. Card summaries carry
  `labels` and `commentCount`, read with one bulk query each.
- **Filters and comments UI:** board filters live in the URL (`?q=&assignee=<id>|none&label=&due=
  overdue|week|none`, `features/boards/filters.ts` translates URL ↔ API); search is debounced
  300 ms and written with `replace`. Each filter combination is its own cache entry under
  `queryKeys.boards.detail(boardId)` (`keepPreviousData` while switching); moves update every
  cached variant. Comments use `useInfiniteQuery` ("Show older comments") and are invalidated
  after any change because offset pages shift. Label colours map to static Tailwind classes in
  `features/labels/colors.ts`.
- **UI shell and feedback (phase 5):** toasts with `sonner` (one `<Toaster>` in `App`, also in
  `renderWithProviders`) for confirmations and move errors; skeletons (`components/ui/Skeleton`)
  instead of "Loading…" text; an `ErrorBoundary` per route (keyed by pathname); a real
  `NotFoundPage` inside the protected layout; `useDocumentTitle` sets "<page> · TaskFlow";
  skip link; account menu (`components/ui/Menu`, WAI-ARIA menu button) with `/account`, which
  updates the display name through `AuthContext.updateUser`. Board pages are full width, other
  pages `max-w-5xl`.
- **Board UX (phase 5):** the open card is in the URL (`?card=<id>`; closing goes back when it
  was opened on the board, otherwise drops the parameter). Sensors: `MouseSensor` (5 px) +
  `TouchSensor` (250 ms press) + keyboard, cards use `touch-manipulation` so swiping scrolls on
  phones; columns snap on small screens. Due dates are red when overdue and amber within two
  days (`features/cards/dueDate.ts`, with screen-reader text). Column actions live in a "⋯"
  menu. On phones the filters fold behind a "Filters (n)" button.
- **Docker (phase 6a):** multi-stage images. `backend/Dockerfile`: dependencies cached in their
  own layer, jar split with Spring Boot layertools, JRE-only runtime as a non-root user,
  healthcheck on `/actuator/health`; tests are not run in the image (CI runs them).
  `frontend/Dockerfile`: Vite build served by nginx; `nginx.conf.template` proxies `/api` to
  `${API_UPSTREAM}` (envsubst at start), falls back to `index.html` for client routes, caches
  hashed assets for a year and never caches `index.html`. Compose profile `app` adds `api` (not
  published) and `web` (`WEB_PORT`, default 3000); plain `docker compose up` stays DB-only. The
  `api` service sets `CORS_ALLOWED_ORIGINS` to the web origin and `COOKIE_SECURE=false`.
- **Quality gates (phase 6b):** JaCoCo `check` fails `verify` below 80% lines / 70% branches
  (MapStruct `*MapperImpl` and `TaskflowApplication` excluded). Vitest V8 coverage minimums:
  75% lines/statements/functions, 65% branches. Both sit a little below the current values.
  CI runs a third job, `e2e`, after backend and frontend: `docker compose --profile app up
  --wait`, Playwright with Chromium, report and container logs uploaded on failure. The web image
  has a healthcheck (`127.0.0.1`, not `localhost`, because Alpine resolves localhost to IPv6).
- **Lessons from the E2E tests:** they found three bugs that component tests could not see
  (all fixed, each with a regression test): React Hook Form returns a string, not a list, for a
  single checkbox (use a `Controller` for checkbox groups); per-call `mutate(..., { onSuccess })`
  callbacks are dropped when the component remounts, so use `mutateAsync` when a mutation can
  remount its own form; and an inline "add" form must clear itself on submit, not when the
  request finishes, and keep its submit button enabled so Enter works.
- **Resource 404s:** a non-member gets the same 404 body as for a missing id ("Board not
  found", "Column not found", "Card not found"), via `WorkspaceAccessService.requireRole(...,
  notFoundMessage)`.
- **Code layout:** package-by-feature in the backend (`auth`, `user`, `workspace`, `board`,
  `card`, `label`, `comment`, `security`, `common`); feature folders in the frontend (`src/app`, `src/features/*`,
  `src/pages`, `src/components/ui`, `src/lib`, `src/types`, `src/test`).

## Commands

```bash
docker compose up -d                 # PostgreSQL on localhost:5432
docker compose --profile app up -d --build  # whole app (db + api + web) on http://localhost:3000
cd backend && ./mvnw spring-boot:run # API on http://localhost:8080 (or PORT from .env); docs at /swagger-ui
cd backend && ./mvnw verify          # build + tests (needs Docker running) + JaCoCo report
cd frontend && npm run dev           # UI on http://localhost:5173
cd frontend && npm run lint && npm run format:check && npm run test && npm run build
cd frontend && npm run test:coverage # with coverage minimums
cd e2e && npm test                   # Playwright, needs the app running on :3000 (E2E_BROWSER_CHANNEL=msedge locally)
```

Backend tests whose class name ends in `Test` are run by Surefire. Integration tests extend
`AbstractIntegrationTest` (shared PostgreSQL Testcontainer) and are named `*IntegrationTest`.
Tests that need signed-in users extend `ApiIntegrationTest` (`registerUser`, `perform(user, …)`).
Board, column and card tests extend `BoardTestSupport` (`createTeam`, `createBoard`,
`createColumn`, `createCard`, `createLabel`, `comment`, `editCard`, `moveCard`, `cardTitles`).
Frontend component tests mock the feature's `api.ts` module with `vi.mock` and render through
`src/test/renderWithProviders.tsx`.
`src/test/apiContract.test.ts` checks the URL, method and body of every API function: add a row
when adding an endpoint. End-to-end tests live in `e2e/` (own package, Playwright); they set up
data through the API (`Api` in `tests/support.ts`), drive the behaviour through the browser, and
register unique users per test so they can run in parallel on a database that has data.
Shared API-shape builders (`sampleBoard`, `fullCard`, `member`, `comment`, `page`, `BUG`, `DOCS`)
live in `src/test/fixtures.ts`.

## Phase status

| # | Phase | Status |
|---|---|---|
| 0 | Setup: monorepo, Spring Boot, Vite, compose, Flyway, CI | Done |
| 1 | Authentication (security code written by Claude at Omar's request) | Done |
| 2 | Workspaces and members | Done |
| 3 | Boards, columns and cards (move logic + optimistic locking by a subagent) | Done |
| 4 | Labels, comments, search and filters | Done — pending merge |
| 5 | Frontend polish | Done — pending merge |
| 6 | Testing (70%+ backend coverage, component tests, Playwright) | Done — pending merge |
| 7 | Docker, CI/CD and deployment, demo user | Not started |
| 8 | Extras: WebSockets, notifications, activity history, dashboard, attachments, dark mode | Not started |
| 9 | README in English | Not started |

Update this table and the decisions above whenever something changes.
