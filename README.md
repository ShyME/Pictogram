# Pictogram

[![CI](https://github.com/ShyME/pictogram/actions/workflows/ci.yml/badge.svg)](https://github.com/ShyME/pictogram/actions/workflows/ci.yml)

An Instagram-like portfolio app — square image posts, a follow graph, and a feed —
built as a **modular monolith** (React 19 + Spring Boot 4 / Java 25) to practise DDD and
TDD. It is built with the help of [Claude Code](https://claude.com/claude-code). Architecture
lives in [`CONTEXT-MAP.md`](./CONTEXT-MAP.md) and [`docs/adr/`](./docs/adr/);
the backend build is described in [`backend/README.md`](./backend/README.md), the frontend
in [`frontend/README.md`](./frontend/README.md).

## What's built

Five bounded contexts, each a Spring Modulith module with its own schema (`social` owns
two, `follow` and `likes`), a published interface, and a `CONTEXT.md` glossary
(indexed from [`CONTEXT-MAP.md`](./CONTEXT-MAP.md)):

| Context | What it does |
|---|---|
| `identity` | Google OIDC sign-in (backend-driven), then Pictogram's own ES256 access + rotating refresh tokens (ADR-0004) |
| `profile` | Username, display name, bio, and the onboarding step that first creates a profile |
| `media` | Uploaded images re-encoded server-side to one canonical square JPEG + thumbnail (ADR-0006); orphan collection |
| `post` | A post — one image plus an optional caption, published by an author; immutable, delete-only |
| `social` | The follow graph (counts, lists), the feed assembled fan-out-on-read behind a port (ADR-0003), and likes on a post (comments designed, not built) — three `internal/` sub-domains in one module (#128) |

The frontend is a React SPA that composes feed cards client-side from batched calls, with no
BFF (ADR-0005). Every context has `@ApplicationModuleTest` coverage; user journeys are
written once as `*Scenarios` and run both in-process and black-box against the built image,
plus a matching set of Playwright browser journeys (ADR-0007).

## How to run it

### Prerequisites

- **Docker** (Compose v2) — for the full stack and for the Testcontainers-backed tests.
- A JDK on `PATH` — only to *launch* Gradle; the Java 25 toolchain the build targets is
  provisioned automatically.
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
task seed        # fill a running `task up` stack with demo data — then sign in as 'alice'
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

A `caddy` container is the single published entrypoint on `:8080` (ADR-0012): it routes
`/ws` to `chat` — a separate service, own Gradle build, own Dockerfile (ADR-0014) — and
everything else to `app`. Neither `app` nor `chat` publishes a port of its own. `chat`
has no message-handling yet; its WebSocket handshake is the only endpoint (#163, #164).

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

Chat — its own Gradle build (ADR-0014), needs no infra:

```bash
cd chat && ./gradlew bootRun                 # task chat
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

### Blackbox tier

The whole stack in containers (the `compose.mock-oauth.yaml` overlay stands in for Google),
exercised two ways: the **`@Tag("blackbox")` backend scenarios** — every `*Scenarios` mixin
that the in-process suite runs, re-run through `ContainerDriver` over HTTP against the built
image — and the **Playwright journeys**. On-demand only in CI. A nondeterministic failure here
is a defect to fix, never a retry: Playwright runs `retries: 0` and the Gradle suite has no
retry.

```bash
task test:blackbox   # both: builds + starts the stack, runs backend blackbox + Playwright, tears down
task test:e2e        # Playwright only
```

Against a stack you keep running (`task up`):

```bash
cd backend  && ./gradlew build -PincludeBlackbox   # backend blackbox scenarios (PICTOGRAM_BASE_URL, default :8080)
cd frontend && pnpm test:e2e                        # Playwright (after `pnpm exec playwright install` once)
```

## Continuous integration

`.github/workflows/ci.yml` is **on-demand only** — the "Run workflow" button in the Actions
tab, never automatically on a PR or a push to `main` (GitHub Actions minutes budget). A
manual run does the full sweep: the backend `./gradlew check` (Modulith `verify()`, unit +
`@ApplicationModuleTest` + in-process `@Tag("fast")` scenarios, and the `openapi.json` drift
check), `chat`'s own `./gradlew check` (a separate Gradle build, ADR-0014 — no
Testcontainers, nothing shared with the backend job), the frontend lint / typecheck / test /
build, the visual-regression suite, and the `blackbox` job — the `@Tag("blackbox")` backend
tests (including a real WebSocket handshake against `chat` through the shared Caddy origin),
a check that that origin fronts both services, and the Playwright journeys against
`compose.yaml` + `compose.mock-oauth.yaml`.

Because nothing runs on push, the everyday gate is local: `./gradlew check` (backend and
chat), `pnpm test` / `pnpm lint` / `pnpm typecheck` (frontend), and `task test:blackbox`
before a merge or a deploy. Trigger the workflow for a clean-runner belt-and-braces run when
it matters.

`main` is protected by the **"Main security"** repository ruleset: no direct pushes, no
force-push, no deletion, linear history, every change via a PR. There are no required status
checks (CI doesn't run on PRs), so merging is not gated on a green run — it's on you to
trigger CI when a change warrants it.

## Deployment

Pictogram deploys to a single VPS running the compose topology behind Caddy, which
terminates TLS for a registered domain (ADR-0012). `compose.prod.yaml` overlays
`compose.yaml` to pull `app`/`chat`/`caddy` images from GHCR instead of building them. The
**Deploy** workflow (`.github/workflows/deploy.yml`) is triggered on demand — the "Run
workflow" button in the Actions tab, never automatically on a push — and builds, pushes
and rolls the box. `scripts/deploy/wizard.sh` walks the one-time box, DNS and
OAuth-client setup.

The full first-deploy sequence, the per-IP edge rate-limit numbers and their rationale,
DNS and OAuth records, rollback and secret rotation are in
[`docs/runbook/deployment.md`](./docs/runbook/deployment.md).

## Before serving real traffic

All of this is handled by the deployment runbook above; it is listed here as the checklist.

- **Signing key.** Set `PICTOGRAM_AUTH_SIGNING_KEY` (a P-256 private JWK). The `prod` profile
  refuses to start without it — an ephemeral key breaks multi-replica token verification and
  logs everyone out on restart. `compose.prod.yaml` makes it a hard `${VAR:?}` guard.
- **Cookie transport.** `compose.yaml` keeps the refresh cookie `Secure` by default; only the
  plain-HTTP localhost stacks (`compose.mock-oauth.yaml`, the `local` profile) opt out. Serve
  the deployed app over TLS.
- **Rate limiting.** The app itself does none. The public unauthenticated surface —
  `GET /api/media/*/original` and `/thumbnail`, `GET /api/profiles/*`, `GET /api/posts`,
  `GET /api/posts/*/comments`, `GET /api/comments`, `GET /api/follows/*`, `POST /api/auth/refresh`,
  `POST /api/posts/*/comments`, and the OIDC start at `/oauth2/authorization/google` — sits behind
  Caddy's per-IP `rate_limit` zones (`Caddyfile.prod`), numbers in the runbook.
- **Media bucket.** `S3BlobStore` auto-creates the bucket on first upload for local dev only.
  Pre-create it during provisioning and withhold `s3:CreateBucket` from the runtime role.
- **OAuth consent screen.** Point it at the static `/privacy.html` and `/terms.html` pages
  (`frontend/public/`), and set the app homepage to `/`.

## Contributing

Formatting is machine-enforced — run `task format` before you push. IDE setup and the
`git blame` cutover config are in [`CONTRIBUTING.md`](./CONTRIBUTING.md).

## Task reference

`task --list` after installing go-task. Common ones: `up`, `up:google`, `seed`, `down`, `dev`,
`backend`, `frontend`, `format`, `test`, `test:e2e`, `test:blackbox`, `logs`, `clean`.

## License

[MIT](./LICENSE).
