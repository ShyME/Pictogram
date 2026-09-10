# Deployment runbook

How Pictogram is deployed to production, and the operational knobs that live nowhere in
the code. Implements ADR-0012 (single VPS + Compose + Caddy) and the edge half of
ADR-0011's rate limiting.

The current host is a **temporary GCP `e2-medium`** on a free-trial credit (ADR-0012
addendum). Migrating to a flat-fee box before the credit runs out is #176.

## Topology

```
                  ┌────────────────────── one VPS ──────────────────────┐
  Internet ──443──▶ caddy (custom image, caddy-ratelimit)                │
                  │   ├─ /ws          ─▶ chat  :8081  (WebFlux, ADR-0014)│
                  │   └─ everything   ─▶ app   :8080  (Spring, the SPA)  │
                  │                          ├─▶ postgres :5432 (volume) │
                  │                          └─▶ minio    :9000 (volume) │
                  └─────────────────────────────────────────────────────┘
```

`compose.yaml` is the base (plain-HTTP `:8080`, local builds). `compose.prod.yaml`
overlays it: image pulls from GHCR instead of builds, `restart: unless-stopped`, only
`caddy` publishes ports (`80`, `443`), named volumes for the Let's Encrypt cert, and every
secret as a hard `${VAR:?}` guard so a missing `.env` value fails the deploy immediately
(ADR-0004 prod gate — no dev default can leak in).

## One-time setup

Run `scripts/deploy/wizard.sh` from a clone. It is idempotent and resumable — re-run it
after fixing anything. It covers:

1. **GCP** — install `gcloud`, `gcloud auth login`, create/select the project, link the
   billing account carrying the credit, enable `compute.googleapis.com`.
2. **VM** — `gcloud compute instances create`: `e2-medium`, Ubuntu LTS, 30 GB balanced
   persistent disk, firewall for `22`/`80`/`443`, a reserved static external IP.
3. **Box** — Docker Engine + the compose plugin, a `deploy` user, `docker login ghcr.io`
   (a GHCR read token), and fetch `compose.yaml`, `compose.prod.yaml`, `Caddyfile.prod`
   into `~/pictogram`.
4. **Signing key** — generate a P-256 JWK, write `PICTOGRAM_AUTH_SIGNING_KEY` to the box
   `.env`, derive `PICTOGRAM_AUTH_PUBLIC_KEY` from it by dropping the `d` member (chat
   verifies signatures, holds no private material — ADR-0014).
5. **DB / MinIO creds** — generate strong values into the box `.env`.
6. **CI secrets** — `DEPLOY_SSH_KEY` and `DEPLOY_USER`. `DEPLOY_HOST` and
   `DEPLOY_KNOWN_HOSTS` are set later, in the domain-gated tail: the workflow rolls the box
   the moment `DEPLOY_HOST` exists, and the box cannot serve until the tail fills
   `PICTOGRAM_DOMAIN` + the OAuth values, so a dispatch before then would only fail on the
   missing `${PICTOGRAM_DOMAIN}` guard.
7. It then **pauses** and prints the domain-gated tail (below), which cannot run until the
   domain is registered.

### Domain-gated tail (needs the registered domain)

Pictogram is served from a **subdomain** — `pictogram.imshy.me` — so the `imshy.me` apex
stays free for a separate site. `PICTOGRAM_DOMAIN` is the full subdomain; nothing in the
app assumes the apex.

- Register the domain (Cloudflare Registrar). The zone's nameservers are Cloudflare's by
  default — DNS is edited in the Cloudflare dashboard.
- **DNS record:** one `A  pictogram  <VM static IP>` (host `pictogram`, not `@`), plus
  `AAAA  pictogram  <VM IPv6>` if the VM has one. Caddy resolves the name directly for the
  ACME challenge on `:80`.
- **Do not proxy the record.** On Cloudflare set it to *DNS only* (grey cloud). A proxied
  record makes Cloudflare terminate `:80`/`:443` itself — Caddy's ACME challenge never
  completes so no certificate issues, and every request then arrives from a Cloudflare IP,
  collapsing all four `{remote_host}` rate-limit zones into one shared bucket.
- **Production Google OAuth client** (Google Cloud console → APIs & Services →
  Credentials): type *Web application*, authorized redirect URI
  `https://<domain>/login/oauth2/code/google`, and a configured OAuth consent screen
  (app name, support email, the `openid` + `email` scopes, the app-homepage
  `https://<domain>/`, the privacy-policy `https://<domain>/privacy.html` and
  terms-of-service `https://<domain>/terms.html` links, publishing status *In production*
  or the test users added). Those two pages are static files under `frontend/public/`
  (`LegalPagesTest` pins the paths). Tests keep `mock-oauth2-server` (ADR-0007) — this
  client is production-only.
- Fill the box `~/pictogram/.env`: `PICTOGRAM_DOMAIN`, `GOOGLE_CLIENT_ID`,
  `GOOGLE_CLIENT_SECRET`.
- Set the CI secrets `DEPLOY_HOST` (the domain) and `DEPLOY_KNOWN_HOSTS` (the box's pinned
  host key, keyed to the domain name). This is what arms the workflow's box-roll step.
- Run the **Deploy** workflow (below). Caddy issues the certificate on first request.

### Co-hosting another site on the `imshy.me` apex

Pictogram's Caddy owns `:80`/`:443` on the box, but only answers for the one hostname in
its `{$PICTOGRAM_DOMAIN}` block. To put a second site on the apex without a second box:

1. Point `A @ <VM static IP>` (DNS only) at the same box.
2. Add a site block to `Caddyfile.prod` for `imshy.me` (and `www.imshy.me`) — static
   files from a mounted volume, or `reverse_proxy` to another container added to
   `compose.prod.yaml`. Caddy issues a separate Let's Encrypt certificate for it
   automatically.
3. Redeploy. Pictogram's block, rate-limit zones and OAuth redirect are keyed on
   `{$PICTOGRAM_DOMAIN}` and are unaffected.

## The deploy workflow

`.github/workflows/deploy.yml`, **`workflow_dispatch` only** — the "Run workflow" button in
the Actions tab. A merge to `main` does not deploy; a human triggers a release when they
choose to (ADR-0012 addendum records the change from the originally-specified post-merge
trigger).

Inputs:

- **`rebuild`** (default `true`) — build and push `app`/`chat`/`caddy` from the selected
  ref before rolling. Set `false` to roll to an image tag already in GHCR without a
  rebuild.
- **`tag`** (default: this run's commit SHA) — the tag to build as / roll to. Set it to a
  prior SHA with `rebuild: false` for a rollback.

Steps:

1. If `rebuild`: buildx build `app`, `chat`, `caddy`, push each to
   `ghcr.io/shyme/pictogram-<svc>` tagged `latest` and `<tag>`.
2. `caddy validate` `Caddyfile.prod` against that caddy image — catches a broken Caddyfile
   or a missing module before it reaches the box.
3. If `DEPLOY_HOST` is set: over SSH, `cd pictogram && export PICTOGRAM_TAG='<tag>' &&
   docker compose -f compose.yaml -f compose.prod.yaml pull && ... up -d --wait`. The
   `tag` input is validated against the Docker tag grammar in the first step, so it cannot
   inject an output key or a remote command.

Until the domain-gated tail sets `DEPLOY_HOST` the roll step is skipped with a notice;
build + push + validate still run, so the pipeline is exercised.

**CI secrets:** `DEPLOY_SSH_KEY` (private key whose public half is in the box `deploy`
user's `authorized_keys`), `DEPLOY_USER` (`deploy`), `DEPLOY_HOST` (the domain; set in the
domain-gated tail), and `DEPLOY_KNOWN_HOSTS` (the box's pinned host key — `ssh-keyscan`
output; without it CI falls back to trust-on-first-use). Rebuilding the box (#176)
invalidates `DEPLOY_HOST`/`DEPLOY_KNOWN_HOSTS` — re-run wizard Stage 12. GHCR push uses the
built-in `GITHUB_TOKEN`
(`packages: write`).

## Edge rate limits

`Caddyfile.prod` runs `rate_limit` zones keyed on `{remote_host}` (the real client — Caddy
is the edge) over the ADR-0011 unauthenticated surface. Exceeding a zone returns `429`.
These are **starting numbers**; retuning them, and adding the in-app per-user limiter, is
#141.

| Zone       | Matches                                                                              | Limit        | Why                                                                                   |
|------------|-------------------------------------------------------------------------------------|--------------|--------------------------------------------------------------------------------------|
| `reads`    | `GET` `/api/media/*`, `/api/profiles/*`, `/api/posts*`, `/api/comments*`, `/api/follows/*` | 60 / min / IP | Anonymous browsing of a profile + feed fires a burst of media and profile GETs; 60/min covers a real visitor scrolling, not a scraper. |
| `refresh`  | `POST /api/auth/refresh`                                                             | 10 / min / IP | The SPA refreshes on load and near token expiry (~15 min) — single digits per minute is generous. Higher would let a stolen refresh cookie be brute-replayed. |
| `comments` | `POST /api/posts/*/comments`                                                         | 6 / min / IP  | A human writes a few comments a minute at most. This is the only unauthenticated-surface write; keep it tight. |
| `oauth`    | `/oauth2/authorization/*`                                                            | 6 / min / IP  | Starting a sign-in redirect is rare per person; a flood here is someone hammering the Google round-trip. |

The `reads` zone is deliberately a superset of the issue's list — it also covers the `GET`
comment endpoints ADR-0011 names, since they are the same read surface.

Tuning: watch Caddy's access log for `429`s. If real users hit `reads`, raise it in steps
of 30 and redeploy; if a single IP sustains hundreds a minute, that is the limiter doing
its job. All four numbers are one-line edits to `Caddyfile.prod` — a normal PR, then run
the Deploy workflow to pick it up.

## Rollback

Images are tagged with the commit SHA and never deleted. To roll back:

```bash
ssh deploy@<host>
cd pictogram
export PICTOGRAM_TAG=<previous good SHA>
docker compose -f compose.yaml -f compose.prod.yaml pull
docker compose -f compose.yaml -f compose.prod.yaml up -d --wait
```

Or run the Deploy workflow with `rebuild: false` and `tag: <previous good SHA>`. Then
revert the offending commit on `main` so the next deploy does not re-ship it.
Postgres migrations are per-module Flyway (ADR-0009) and forward-only — a rollback that
needs a schema undo is a manual `pg_dump` restore, not covered here.

## Secret rotation

All secrets live only in the box `~/pictogram/.env`. To rotate one: edit `.env`, then
`docker compose -f compose.yaml -f compose.prod.yaml up -d`.

- **Signing key** (`PICTOGRAM_AUTH_SIGNING_KEY`): generate a new P-256 JWK, update both it
  and the derived `PICTOGRAM_AUTH_PUBLIC_KEY`, roll `app` and `chat` together. Every
  active session is logged out (access tokens stop verifying); acceptable.
- **Google client secret**: rotate in the Google console, paste the new value, roll `app`.
- **DB / MinIO passwords**: change in Postgres/MinIO first, then `.env`, then roll.

## Backups

Deferred (ADR-0012). v1 data is re-seedable (`task seed`). When the backup ticket lands:
whole-box snapshots plus a periodic `pg_dump` retained off the box.

## Smoke test (after the first live deploy)

Against `https://<domain>`:

- [ ] Google sign-in completes and lands back on `/`.
- [ ] Publish a post — image lands in MinIO, appears in the grid.
- [ ] Follow another account; the feed updates.
- [ ] Like a post; the count changes.
- [ ] Comment on a post; it appears, and the count updates.
- [ ] Open the chat overlay from a profile; a message relays over `wss://<domain>/ws`.
- [ ] `pictogram_refresh` cookie has `Secure` (DevTools → Application → Cookies).
- [ ] A burst of >60 `GET /api/posts` in a minute from one IP starts returning `429`.

## Migrating off GCP (#176)

Before the free-trial credit expires:

1. Provision the replacement box (Hetzner CX22 or equivalent) — same wizard, GCP steps
   skipped.
2. Copy `~/pictogram/.env` across.
3. `docker compose ... pull && up -d --wait` on the new box.
4. Repoint the `A`/`AAAA` records; wait for the new Caddy to issue its cert.
5. Update `DEPLOY_HOST` if it changed, and refresh `DEPLOY_KNOWN_HOSTS` (and
   `DEPLOY_SSH_KEY` if the key changed) — the new box has a new host key.
6. Tear down the GCP project.
