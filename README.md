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

One image serves the API and the built SPA at a single origin.

```bash
docker compose up --build        # or: task up
```

Open <http://localhost:8080>. `backend/Dockerfile` is a three-stage build: it compiles the
SPA, bundles it into the Spring Boot jar as static resources, and runs it on the `prod`
profile against the `postgres` and `minio` services. Spring serves the SPA at `/`.

Client-side deep links (a browser reload on a route other than `/`) will 404 until the
frontend grows real nested routes and a matching `index.html` fallback is added.

```bash
docker compose down              # stop      (task down)
docker compose down -v           # stop + wipe data
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

Playwright drives the whole stack in containers. `main`-only in CI; run it locally against
a live `compose.yaml`:

```bash
task up                                       # start the app
cd frontend && pnpm exec playwright install   # first run only
cd frontend && pnpm test:e2e                  # or: task test:e2e
```

## Continuous integration

`.github/workflows/ci.yml` gates every PR: the backend `./gradlew build` (Modulith
`verify()`, unit + `@ApplicationModuleTest` + in-process `@Tag("fast")` scenarios, and the
`openapi.json` drift check) and the frontend lint / typecheck / test / build.

The push to `main` runs **only** the `blackbox` job — the `@Tag("blackbox")` backend tests
and the Playwright journeys against `compose.yaml`. With "require branches up to date before
merging" on, the merged tree already passed the backend/frontend suites on the PR, so those
don't re-run. `workflow_dispatch` forces a full run.

## Task reference

`task --list` after installing go-task. Common ones: `up`, `down`, `dev`, `backend`,
`frontend`, `test`, `logs`, `clean`.
