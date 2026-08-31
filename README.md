# Pictogram

[![CI](https://github.com/ShyME/pictogram/actions/workflows/ci.yml/badge.svg)](https://github.com/ShyME/pictogram/actions/workflows/ci.yml)

An Instagram-like portfolio app — square image posts, a follow graph, and a feed —
built as a **modular monolith** (React 19 + Spring Boot 4 / Java 25) to practise DDD and
TDD. Architecture lives in [`CONTEXT-MAP.md`](./CONTEXT-MAP.md) and [`docs/adr/`](./docs/adr/);
the backend build is described in [`backend/README.md`](./backend/README.md), the frontend
in [`frontend/README.md`](./frontend/README.md).

## How to run it

### Prerequisites

- **Docker** (Compose v2) — for the full stack and for the Testcontainers-backed tests.
- A JVM 21+ on `PATH` — only to *launch* Gradle; the JDK 25 toolchain is auto-provisioned.
- **Node 24** + Corepack (`corepack enable`) — only for host frontend development.
- Optional: [go-task](https://taskfile.dev) (`brew install go-task`) for the shortcuts below.

Copy the env template once:

```bash
cp .env.example .env
```

`.env` is gitignored and configures `compose.yaml` (the container run). The host-dev path
(`compose.dev.yaml` + the `local` profile) uses fixed `pictogram`/`pictogram` credentials.

### Whole app in containers

One image serves the API and the built SPA at a single origin. `compose.yaml` has no OIDC
provider on its own — pick how Google sign-in is wired:

```bash
task up          # bundled mock Google — sign in with any username, no account needed
task up:google   # real Google — needs GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET in .env
```

Under the hood each overlays `compose.yaml`:

```bash
docker compose -f compose.yaml -f compose.mock-oauth.yaml up --build -d --wait
docker compose -f compose.yaml -f compose.google.yaml     up --build -d --wait
```

For `up:google`, create an OAuth 2.0 "Web application" client in the Google Cloud console
with the redirect URI `http://localhost:8080/login/oauth2/code/google`, then put the id and
secret in `.env` (see `.env.example`).

Open <http://localhost:8080>. `backend/Dockerfile` is a three-stage build: it compiles the
SPA, bundles it into the Spring Boot jar as static resources, and runs it on the `prod`
profile against the `postgres` and `minio` services. Spring serves the SPA at `/`, and a
hard reload or pasted link on a client route (`/login`, `/onboarding`) forwards to
`index.html` so the SPA re-resolves it.

```bash
docker compose -f compose.yaml down --remove-orphans     # stop      (task down)
docker compose -f compose.yaml down -v --remove-orphans   # stop + wipe data
```

### Apps on the host (development)

Infra in containers, the apps on the host with hot reload:

```bash
docker compose -f compose.dev.yaml up -d     # Postgres + MinIO + mock-oauth2-server
                                             #   (task dev)
```

Backend — the `local` profile auto-starts that same infra via
`spring-boot-docker-compose`, so this alone is enough:

```bash
cd backend && ./gradlew :app:bootRun         # task backend
```

Frontend — the Vite dev server proxies `/api`, `/oauth2` and `/login/oauth2` to the
host backend:

```bash
cd frontend && pnpm install && pnpm dev      # task frontend
```

In IntelliJ, running the app picks up the `local` profile and the infra starts on its
own — see `backend/README.md` for the run-configuration setup.

## Spring profiles

| Profile | Where | Datasource | Notes |
|---|---|---|---|
| `local` | host dev (`bootRun`, IntelliJ) | `localhost:5432` | starts `compose.dev.yaml` infra; verbose actuator |
| `test`  | automated tests | Testcontainers | plain-text logs; defined in `:test-support` |
| `prod`  | `compose.yaml` / any container | `PICTOGRAM_DB_*` env | minimal actuator exposure |

## Tests

```bash
task test                        # everything
cd backend && ./gradlew build    # backend + module-boundary check
cd frontend && pnpm test         # frontend
```

### Blackbox journeys

Playwright drives the whole stack in containers (the `compose.mock-oauth.yaml` overlay
stands in for Google). `main`-only in CI; run it locally with:

```bash
task test:e2e     # builds + starts the stack, runs the journeys, tears it down
```

Or against a stack you keep running (`task up`), `cd frontend && pnpm test:e2e`
(after `pnpm exec playwright install` once).

## Continuous integration

`.github/workflows/ci.yml` gates every PR: the backend `./gradlew build` (Modulith
`verify()`, unit + `@ApplicationModuleTest` + in-process `@Tag("fast")` scenarios, and the
`openapi.json` drift check) and the frontend lint / typecheck / test / build.

The push to `main` runs **only** the `blackbox` job — the `@Tag("blackbox")` backend tests
and the Playwright journeys against `compose.yaml` + `compose.mock-oauth.yaml`. With
"require branches up to date before merging" on, the merged tree already passed the
backend/frontend suites on the PR, so those don't re-run. `workflow_dispatch` forces a
full run.

## Task reference

`task --list` after installing go-task. Common ones: `up`, `up:google`, `down`, `dev`, `backend`,
`frontend`, `test`, `logs`, `clean`.
