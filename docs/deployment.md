# Deploying TaskFlow

```
Browser ──► Vercel (static frontend, <app>.vercel.app)
               │  /api/*  rewritten to the API: same origin for the browser, so the refresh
               ▼  cookie is first-party and there is no CORS preflight
            Render (API, Docker image from backend/Dockerfile)
               ▼
            Neon (PostgreSQL)
```

Everything below uses free plans. Two consequences worth knowing:

- **Render free web services sleep after 15 minutes without traffic** and take about a minute
  to wake up. The frontend shows "Waking up the server…" during that minute.
- **Neon** is used instead of Render's free PostgreSQL, which expires 30 days after creation.
  Neon's free database does not expire; its compute sleeps after 5 minutes idle and wakes in
  well under a second.

Pick regions close to each other: for example Render **Virginia (US East)** and Neon **AWS US
East (N. Virginia)**. Every query crosses between them.

## 1. Database (Neon)

1. Create a project at <https://neon.com>. Choose the region (see above) and PostgreSQL 17.
2. Open **Connect**, turn **Connection pooling off** (the API keeps its own small pool), and
   copy the parts of the connection string
   `postgresql://<user>:<password>@<host>/<database>?sslmode=require`.
3. You will give the API three values:

   | Variable      | Value                                                 |
   | ------------- | ----------------------------------------------------- |
   | `DB_URL`      | `jdbc:postgresql://<host>/<database>?sslmode=require` |
   | `DB_USERNAME` | `<user>`                                              |
   | `DB_PASSWORD` | `<password>`                                          |

Flyway creates the tables on the first start of the API; there is nothing to run by hand.

## 2. Signing key

Generate a new key for production (never reuse the one from `.env.example`):

```bash
openssl rand -base64 32
```

## 3. API (Render)

1. At <https://render.com>: **New → Web Service**, connect the GitHub repository.
2. Settings:

   | Setting            | Value                                                      |
   | ------------------ | ---------------------------------------------------------- |
   | Name               | `taskflow-api` (gives `https://taskflow-api.onrender.com`) |
   | Region             | the one chosen above                                       |
   | Branch             | `main`                                                     |
   | Root directory     | `backend`                                                  |
   | Runtime / language | Docker (it finds `backend/Dockerfile`)                     |
   | Instance type      | Free                                                       |
   | Health check path  | `/actuator/health`                                         |
   | Auto-Deploy        | **Off**: GitHub Actions deploys after the tests pass       |

3. Environment variables:

   | Variable                               | Value                                                  |
   | -------------------------------------- | ------------------------------------------------------ |
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | from step 1                                            |
   | `JWT_SECRET`                           | from step 2                                            |
   | `CORS_ALLOWED_ORIGINS`                 | `https://<your-app>.vercel.app` (fill in after step 5) |
   | `DEMO_ENABLED`                         | `true`                                                 |

   Leave `PORT` unset (Render provides it) and `COOKIE_SECURE` unset (secure is the default).

4. Create the service and wait for the first deploy. `https://taskflow-api.onrender.com/actuator/health`
   must answer `{"status":"UP"}`.
5. **Settings → Deploy Hook**: copy the URL. It is a secret: anyone with it can trigger deploys.

If Render gives the service a different URL, use that URL everywhere below and in
`frontend/vercel.json`.

## 4. Continuous deployment (GitHub)

In the repository: **Settings → Secrets and variables → Actions**.

| Kind     | Name                     | Value                               |
| -------- | ------------------------ | ----------------------------------- |
| Secret   | `RENDER_DEPLOY_HOOK_URL` | the deploy hook from step 3.5       |
| Variable | `API_URL`                | `https://taskflow-api.onrender.com` |

From then on every push to `main` runs the tests and, if they all pass, the `deploy` job calls the
hook and waits until `/actuator/info` reports the pushed commit. Without these values the job
skips itself with a notice.

## 5. Frontend (Vercel)

1. At <https://vercel.com>: **Add New → Project**, import the repository.
2. Settings: **Root Directory** `frontend`; framework preset **Vite** (build `npm run build`,
   output `dist`, both detected); Node.js version: the newest offered.
3. No environment variables are needed: the frontend calls the relative path `/api`, and
   `frontend/vercel.json` rewrites it to the API.
4. Deploy. Note the production URL (`https://<your-app>.vercel.app`).
5. Back in Render, set `CORS_ALLOWED_ORIGINS` to that URL and save (Render restarts the API).
   Browsers send an `Origin` header even on these proxied requests, so this is required.

`frontend/vercel.json` also sets the security headers (Content-Security-Policy and friends), the
cache policy for hashed assets, and turns off preview deployments: only `main` is deployed.

## 6. Check

- Open the Vercel URL and click **Try the demo**.
- Register an account, create a board, reload: the session survives (refresh cookie).
- `https://taskflow-api.onrender.com/actuator/info` shows the deployed commit.

## Troubleshooting

| Symptom                       | Likely cause                                                                                                        |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| First request takes ~1 minute | The free API was asleep. Expected.                                                                                  |
| `403` on login or register    | `CORS_ALLOWED_ORIGINS` does not contain the exact Vercel URL (scheme, no trailing slash).                           |
| Signed out after every reload | The API is not reached through the Vercel rewrite (cookie not first-party), or `COOKIE_SECURE=false` in production. |
| API fails to start            | Check the Render logs: usually a wrong `DB_URL` (it must start with `jdbc:`) or a missing `JWT_SECRET`.             |
| `deploy` job times out        | The Render build failed (see its logs), or the service is not on `main`.                                            |
