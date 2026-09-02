# CSRF stays off by design; the refresh cookie goes SameSite=Strict; edge hardening is documented

The v1 pre-public security review (#112, findings `SEC-*`) flagged that CSRF protection is
`.disable()`d on every filter chain with no recorded rationale, and asked whether the
refresh cookie should be `SameSite=Strict`. This ADR records why CSRF is off, moves the
refresh cookie to `SameSite=Strict`, and captures the deployment-time controls the codebase
can only document (rate limiting, media-bucket provisioning).

## CSRF is disabled on every chain — deliberately

There are four filter chains, and none carries a state-changing request that is both
authenticated by an ambient credential (a cookie or a servlet session) **and** left
unprotected by something other than a CSRF token:

- **`/api/**` (app, `@Order(1)`)** — `SessionCreationPolicy.STATELESS`, OAuth2 resource
  server. Every mutating call is authorised by a Pictogram access token in the
  `Authorization: Bearer` header. The SPA holds that token in memory (ADR-0004); a
  cross-site page can neither read it nor make the browser attach it, which is exactly the
  property CSRF tokens exist to recover. A CSRF token here guards nothing.
- **`/api/auth/**` (identity, `@Order(-2)`)** — stateless, `permitAll`. This is the only
  cookie-authenticated surface: `POST /api/auth/refresh` reads the `pictogram_refresh`
  httpOnly cookie, and `POST /api/auth/logout`. Cross-site protection comes from the cookie
  attributes — `SameSite` plus `Path=/api/auth` — not from a token (see the next section).
  `refresh` also rotates the token on every call and its response body (the new access
  token) is not readable cross-origin, so a blind cross-site POST achieves nothing an
  attacker can use: at worst it spends the victim's refresh cookie once and the SPA
  silently re-refreshes.
- **`/oauth2/**`, `/login/oauth2/**` (identity, `@Order(-1)`)** — Spring Security's
  `oauth2Login`. Login CSRF is mitigated by the OAuth2 `state` parameter, which Spring
  generates and verifies as part of the Authorization Code flow.
- **SPA / static (app, `@Order(2)`)** — `permitAll`, serves `GET` only. No state to forge.

Turning Spring's `CsrfFilter` on would mean a `CookieCsrfTokenRepository`, a
double-submit/`X-XSRF-TOKEN` handshake threaded through every mutating `fetch` in the SPA,
and a CSRF filter added to two stateless chains — for no request it protects that the
bearer model and `SameSite` do not already cover.

## The refresh cookie moves to SameSite=Strict

`SEC-3` asked for `SameSite=Strict` on `pictogram_refresh`; this ADR adopts it (amending
ADR-0004, which specified `Lax`). The cookie is only ever presented by same-origin `fetch`
from the SPA to `/api/auth/refresh` and `/api/auth/logout` — never by a top-level
navigation — so `Strict` costs nothing and removes the residual cross-site-POST surface
that `Lax` leaves. The post-sign-in redirect still works: the browser lands on `/` after
the Google round-trip without needing the cookie, then the SPA's first same-origin
`refresh` call carries it.

Encoded in `RefreshCookieTest` and `SignInCompletionTest`.

## Deployment-time controls this repo can only document

- **Rate limiting (`SEC-4`)** — the application does none. The public unauthenticated
  surface (`GET /api/media/*/original` and `/thumbnail`, `GET /api/profiles/*`,
  `GET /api/posts`, `GET /api/follows/*`, `POST /api/auth/refresh`, and
  `/oauth2/authorization/google`) needs a reverse-proxy or platform rate limit before real
  traffic. Noted in the README.
- **Media bucket (`SEC-8`)** — `S3BlobStore.ensureBucket()` creates the bucket on first
  upload. That is a local-dev convenience; against real S3 the bucket is pre-created during
  provisioning and `s3:CreateBucket` is withheld from the runtime role, so the branch never
  executes in production. There is no infrastructure-as-code in the repo, so this is a
  runbook note only (README, `application.yml`, and a comment at the call site).

## Consequences

- `pictogram_refresh` is `HttpOnly; Secure; SameSite=Strict; Path=/api/auth`. `Secure` is
  the default (`pictogram.auth.cookie-secure`), opted out of only on the plain-HTTP
  localhost stacks.
- CSP is served on the app chains (`SEC-5`): `script-src 'self'` (the Vite build emits no
  inline scripts), `style-src 'self' 'unsafe-inline'`, `img-src 'self' blob: data:`,
  `object-src 'none'`, `frame-ancestors 'none'`, `form-action 'self'`, plus `Referrer-Policy`
  and `Permissions-Policy`. The identity chains (`/api/auth/**`, `/oauth2/**`) do not yet
  carry these headers — a follow-up.
- If the SPA ever adds a cookie-authenticated, non-rotating mutation, or a same-site
  subdomain that could host it, revisit CSRF for the `/api/auth/**` chain specifically.

## DECISION MADE — please confirm

The ticket owner's comments on #112 asked to *enable CSRF* and *add rate limiting*
outright. This ADR implements the parts that are self-contained and low-risk
(`SameSite=Strict`, security headers, the `prod` signing-key gate, the actuator bind guard)
and asks for a decision on the rest:

1. **CSRF** — recommendation: treat `SameSite=Strict` + the in-memory bearer token as the
   CSRF defense for v1 and keep Spring's `CsrfFilter` off. If defense-in-depth on the
   cookie endpoints is still wanted, the smallest version is a `CookieCsrfTokenRepository`
   on the identity `/api/auth/**` chain only, with the SPA echoing `X-XSRF-TOKEN` on
   `refresh` / `logout` — a dedicated follow-up ticket (backend + frontend + tests), not
   part of #112. **Confirm: accept SameSite=Strict for v1, or schedule the follow-up?**
2. **Rate limiting** — recommendation: enforce it at the edge (reverse proxy / platform),
   which is where per-IP limits belong and which protects the process before a request
   reaches the JVM. An in-app limiter (e.g. a bucket4j filter on `/api/auth/refresh` and
   the OIDC start) would be a separate ticket with its own tuning and tests. **Confirm:
   edge-only for v1, or also build the in-app limiter?**
3. **CSP `style-src`** — kept `'unsafe-inline'` for styles (scripts are strict). Removing it
   needs a nonce/hash pipeline in the Vite build. **Confirm `'unsafe-inline'` for styles is
   acceptable for v1.**
4. **Media bucket (`SEC-8`)** — documented only; there is no IaC to change. **Confirm the
   provisioning runbook will pre-create the bucket and drop `s3:CreateBucket` from the
   runtime role.**
