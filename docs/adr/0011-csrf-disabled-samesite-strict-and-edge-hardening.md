# CSRF: off on the stateless chains, double-submit on the identity chain; refresh cookie SameSite=Strict; edge hardening documented

The v1 pre-public security review (#112, findings `SEC-*`) flagged that CSRF protection is
`.disable()`d on every filter chain with no recorded rationale, and asked whether the
refresh cookie should be `SameSite=Strict`. This ADR records the CSRF posture per chain,
moves the refresh cookie to `SameSite=Strict`, and captures the deployment-time controls
the codebase can only document (rate limiting, media-bucket provisioning).

## CSRF is off on the stateless chains; the identity chain double-submits a token

There are four filter chains. Three disable `CsrfFilter` explicitly (`.csrf(csrf -> csrf.disable())`,
including the `oauth2Login` chain, which would otherwise inherit the framework-default
session-backed filter) — none of them carries a state-changing request authenticated by an
ambient credential that a token would guard. The fourth, the cookie-authenticated identity
chain, runs a double-submit token as defence-in-depth (#125).

- **`/api/**` (app, `@Order(1)`)** — CSRF off. `SessionCreationPolicy.STATELESS`, OAuth2
  resource server. Every mutating call is authorised by a Pictogram access token in the
  `Authorization: Bearer` header. The SPA holds that token in memory (ADR-0004); a
  cross-site page can neither read it nor make the browser attach it, which is exactly the
  property CSRF tokens exist to recover. A CSRF token here guards nothing.
- **`/api/auth/**` (identity, `@Order(-2)`)** — CSRF **on**: a `CookieCsrfTokenRepository`
  (a non-HttpOnly `XSRF-TOKEN` cookie the SPA echoes in `X-XSRF-TOKEN`). This is the only
  cookie-authenticated surface: `POST /api/auth/refresh` reads the `pictogram_refresh`
  httpOnly cookie, and `POST /api/auth/logout`. `SameSite=Strict` + `Path=/api/auth` +
  refresh-token rotation already close the cross-site gap for v1 (see the next section);
  the token is belt-and-braces so a future same-site subdomain, or a cookie-authenticated
  mutation that does not rotate, cannot reopen the gap silently. The chain serves no
  server-rendered form, so the plain `CsrfTokenRequestAttributeHandler` is wired rather than
  the XOR/BREACH variant — the SPA reads the raw cookie value and sends it back verbatim.
  There is no safe endpoint on this chain to prime the cookie, so a cold client's first
  `refresh` is rejected `403` with a freshly seeded `XSRF-TOKEN`; the SPA (and the scenario
  test driver) retry once with it, and every later call finds the cookie already there.
- **`/oauth2/**`, `/login/oauth2/**` (identity, `@Order(-1)`)** — CSRF off. Spring Security's
  `oauth2Login`; login CSRF is mitigated by the OAuth2 `state` parameter, which Spring
  generates and verifies as part of the Authorization Code flow.
- **SPA / static (app, `@Order(2)`)** — CSRF off. `permitAll`, serves `GET` only. No state
  to forge.

Reject path (missing / mismatched token → `403`) pinned by `AuthCsrfTest`; the accept path —
a real echoed token through the whole sign-in / rotate / replay flow — by `GoogleSignInWebTest`.

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
  inline scripts), `style-src 'self'` (no `'unsafe-inline'` — see decision 3),
  `img-src 'self' blob: data:`, `object-src 'none'`, `frame-ancestors 'none'`,
  `form-action 'self'`, plus `Referrer-Policy` and `Permissions-Policy`. The identity chains
  (`/api/auth/**`, `/oauth2/**`) do not yet carry these headers — a follow-up.
- The `/api/auth/**` chain double-submits an `XSRF-TOKEN` / `X-XSRF-TOKEN` pair (#125). A new
  cookie-authenticated mutation belongs on that chain so the token covers it; putting one on a
  chain that runs no `CsrfFilter` needs this ADR revisited first.

## Decisions

The ticket owner's comments on #112 asked to *enable CSRF* and *add rate limiting*
outright. This ADR implemented the self-contained, low-risk parts (`SameSite=Strict`,
security headers, the `prod` signing-key gate, the actuator bind guard). The rest was
carried into #123 and settled there:

1. **CSRF** — `SameSite=Strict` + the in-memory bearer token is the CSRF defense for v1.
   Defense-in-depth on the `/api/auth/**` chain — a `CookieCsrfTokenRepository` plus the SPA
   echoing `X-XSRF-TOKEN` on `refresh` / `logout` — is **implemented** (#125), before the
   repo and app go public. See the per-chain section above.
2. **Rate limiting** — edge-only for v1: the reverse proxy / platform enforces per-IP
   limits on the public unauthenticated surface, with the numbers recorded in the deploy
   runbook. An in-app bucket4j limiter is deferred (#126) — picked up only if the deploy
   target lacks an edge limiter or a concrete abuse pattern appears.
3. **CSP `style-src`** — tightened to `'self'`; `'unsafe-inline'` is gone. The one
   runtime-computed style (the crop image position in `SquareCropper`) stays a React
   `style={}` prop: React applies it through the CSSOM property API (`node.style[prop] =
   value`), which `style-src` does not govern — only `<style>`/`<link>` elements and inline
   `style=` attributes are. So no nonce/hash pipeline was needed. Pinned by
   `SecurityHeadersTest` and `publishPost.spec.ts`.
4. **Media bucket (`SEC-8`)** — documented only; provisioning pre-creates the bucket and
   withholds `s3:CreateBucket` from the runtime role.
