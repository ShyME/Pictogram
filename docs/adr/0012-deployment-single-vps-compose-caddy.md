# Deployment: a single VPS running the compose stack behind Caddy

- **Status:** Accepted
- **Relates to:** ADR-0011 (edge rate limiting, the `Secure` cookie flag), ADR-0004 (the
  `prod` signing-key gate), ADR-0007 (post-merge CI jobs, mock-oauth2-server in tests),
  ADR-0014 (chat's concrete container and deployment shape)

Pictogram has only ever run locally — `compose.yaml` plus an OIDC overlay
(`compose.mock-oauth.yaml` / `compose.google.yaml`) or `compose.dev.yaml` for host-based
development. It will be deployed publicly and the repository made public (settled in #123).
This records the target and why it was chosen over a managed platform.

## One small VPS, the compose topology unchanged, Caddy in front

- **Host:** one VPS in the Hetzner CX22 class — 2 vCPU / 4 GB, a flat ~€4/month with 20 TB
  of traffic included. Oracle Cloud's Always Free ARM tier is the same architecture for €0
  and is an acceptable substitute; the decision is "a flat-fee box", not the specific
  vendor.
- **Runtime:** Docker Compose, the exact `compose.yaml` topology — `app`, `chat`,
  `postgres`, `minio`, all containers on the box, each with a named volume. No service is
  peeled off to a managed offering.
- **Edge:** Caddy as reverse proxy and the single published entrypoint, terminating TLS
  with one automatic Let's Encrypt certificate for a registered domain (~$10/year,
  Cloudflare Registrar or Porkbun; DNS an A record at the box). One site block fronts
  both services — chat's WebSocket path to `chat`, every other request to `app` — so
  `app` and `chat` are internal-only and there is one origin and one certificate, not two
  (#169, ADR-0014). `app` runs with `forward-headers-strategy: native` so redirect URIs
  and the OAuth2 callback are built from Caddy's external `https` scheme and host,
  trusting `X-Forwarded-*` only from the compose network. Terminating TLS for the real
  domain — binding `:443`, mapping the host ports — is layered on in a prod compose
  overlay when the deploy job lands; the committed `compose.yaml` is the plain-HTTP
  `:8080` shape.
- **Deploy:** a GitHub Actions job on push to `main`, after the existing build and blackbox
  jobs — build the image, push to GHCR, then over SSH `docker compose pull && docker
  compose up -d` on the box.

## Why a VPS and not a managed platform

- **Cost predictability was the deciding constraint.** A flat monthly fee has no metered
  surprises. Every container platform that runs Docker images either bills by usage
  (Cloud Run, Fly.io, Railway past its cap) or stacks flat per-service fees (Render:
  separate charges for the web service and the database). The owner is cost-averse; a fixed
  ~€4 removes the fear entirely.
- **The compose topology maps one-to-one.** `compose.yaml` already parameterises every
  secret and endpoint. Deploying is running the same file with a real `.env`. A PaaS would
  force Postgres and object storage out into managed services and a rewrite of how the app
  is wired.
- **Persistent connections.** Chat (ADR-0014) needs long-lived WebSocket connections, one
  per signed-in session. Scale-to-zero and request-scoped platforms (Cloud Run, Render's
  free tier) handle those poorly — cold starts drop sockets, request timeouts sever them.
  Choosing a plain box avoided migrating off a platform that cannot host chat; chat now
  ships as another container on the same box, no change to this ADR's shape.
- **The ops surface is small and is itself portfolio-positive** — a documented box, one
  compose file, one reverse proxy.

Managed Postgres and managed S3 were considered and rejected on the same
cost-predictability grounds: a container Postgres on a volume is sufficient at v1 scale
(single-digit users), and MinIO is already the settled object store (#87). Kubernetes is
overkill for one box.

## What the repository holds, and what it does not

There is **no infrastructure-as-code** — consistent with ADR-0011's stance that
provisioning is a runbook, not repo content. A wizard walks the one-time setup: provision
the box, point DNS, install Docker + Caddy, create the production Google OAuth client
(a real client, production redirect URI, configured consent screen — tests keep
`mock-oauth2-server` per ADR-0007), and write the box's `.env`:
`PICTOGRAM_AUTH_SIGNING_KEY` (a real key — the `compose.yaml` dev default is never
inherited, satisfying ADR-0004's `prod` gate), the database credentials, the MinIO
credentials, and the Google client secret.

## Edge rate limiting

Caddy enforces per-IP limits on the public unauthenticated surface listed in ADR-0011
(`GET /api/media/*`, `GET /api/profiles/*`, `GET /api/posts`, `GET /api/follows/*`,
`POST /api/auth/refresh`, `/oauth2/authorization/google`, and — once comments land —
`POST /api/posts/*/comments`). This is the "edge" half of the rate-limiting work; the
in-app write-path limiter is the other half, and both are owned by one deferrable ticket
that supersedes #126. The concrete numbers live in the deploy runbook.

`GET /api/notifications/unread-count` (#198) is authenticated so it is not on that list,
but the SPA bell polls it ~every 30s per session — a per-user Caddy limit on it belongs in
the same deferred ticket (see ADR-0011).

## Backups

Deferred to a separate ticket — v1 portfolio data is low-stakes and losing it is
recoverable by re-seeding. When that ticket lands: whole-box snapshots (Hetzner's are
~€0.80/month) plus a periodic `pg_dump` retained off the box.

## Consequences

- HTTPS everywhere means `pictogram_refresh`'s `Secure` flag (ADR-0011) is finally true in
  a running environment; `pictogram.auth.cookie-secure` is left at its default.
- One box is a single point of failure. Acceptable for a portfolio deployment; revisit if
  uptime starts to matter.
- The deploy job runs **on demand** (see the 2026-09-08 addendum), the same fix-forward
  model as the blackbox job (ADR-0007) — a bad deploy is rolled forward, not blocked.
- Secrets live only on the box. Rotating the signing key or the OAuth secret is a manual
  edit to the box's `.env` plus a `compose up -d`.

## Addendum (2026-09-08): the first host is a temporary GCP `e2-medium`

The owner has a $300 GCP free-trial credit and chose to spend it as an explicitly
**temporary** host rather than pay a flat fee from day one. A GCP `e2-medium` (2 vCPU /
4 GB, ~$28/month all-in) is architecturally identical to the CX22 class this ADR
specifies — single VM, Docker Compose, Caddy, named volumes, no managed services — so the
shape above is unchanged. The 4 GB is deliberate: two JVMs (`app` + `chat`, ADR-0014) plus
Postgres and MinIO do not fit in 2 GB.

The credit buys roughly ten months. GCP is metered once it expires, so **migrating off GCP
onto a flat-fee box before then is mandatory** — tracked as #176, blocked by the initial
deployment (#140). The migration is a box swap, not a re-architecture: provision the new
host, run the same `compose.yaml` + `compose.prod.yaml` with the same `.env`, repoint DNS.

Images still build in GitHub Actions and push to GHCR; the box only pulls, so its
RAM/CPU is spent on runtime, not builds.

### The deploy trigger is on-demand, not post-merge

The ADR body and consequences specify a deploy job that runs automatically on every push
to `main`. That is changed here to **`workflow_dispatch` only** (`.github/workflows/deploy.yml`,
the "Run workflow" button): a merge to `main` never rolls the box — a human decides when a
release happens. The rationale for the automatic trigger (fix-forward, no branch
protection) still holds for what it protects against, but for a single-box portfolio
deployment the owner prefers an explicit release step over every merge reaching
production. The job still builds, pushes and validates on dispatch, and takes a `tag`
input so a prior image can be re-rolled for rollback without a rebuild.
