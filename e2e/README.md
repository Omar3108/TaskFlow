# TaskFlow end-to-end tests

Three user flows run in a real browser against the whole app in Docker:

1. `board-flow` — register, create a workspace and a board, add cards, drag one to Done, reload.
2. `invitation-flow` — invite by e-mail, sign up from the link, accept, member permissions.
3. `card-collaboration` — labels, comments, filters and the edit conflict between two people.

```bash
docker compose --profile app up -d --build   # from the repository root
cd e2e
npm ci
npx playwright install chromium              # once; or set E2E_BROWSER_CHANNEL=msedge
npm test
npm run report                               # HTML report with traces of failures
```

Every test registers its own users, so the tests are independent and can run in parallel
against a database that already has data.
