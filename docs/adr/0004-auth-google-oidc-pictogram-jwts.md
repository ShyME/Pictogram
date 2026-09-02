# Authentication: Google-only OIDC, backend-driven, Pictogram-issued JWTs

- **Status:** Accepted
- **Relates to:** ADR-0001 (stateless tokens fit the service-extraction goal), ADR-0008 (the `:app` resource-server chain), ADR-0011 (CSRF posture and the `SameSite=Strict` refresh cookie)

Users authenticate **only via Google** in v1, using the OIDC Authorization Code flow with
PKCE handled **server-side** by Spring Security's OAuth2 Client — the browser just follows a
"Continue with Google" link and the client secret never leaves the backend. After verifying
the Google identity, the `identity` module mints **Pictogram's own** access and refresh
JWTs. The access token is signed with **ES256** (ECDSA on the P-256 curve). No other module
ever sees a Google token. An `IdentityProvider` seam in `identity` allows adding
email/password later.

Chosen because it removes all password-handling surface (hashing, resets, verification,
lockout) from v1; because stateless Pictogram tokens verified with a public key fit the
service-extraction goal in ADR-0001; and because a future extracted service can verify
tokens with the public key while holding no signing material. `mock-oauth2-server` stands
in for Google in tests.

**ES256, not EdDSA/Ed25519.** Both are asymmetric, so every property this ADR relies on
holds either way — a service extracted from the monolith verifies with the public key and
holds no signing material. ES256 wins only on tooling: Spring Security 7's `NimbusJwtEncoder`
still rejects EdDSA
([spring-security#17098](https://github.com/spring-projects/spring-security/issues/17098)),
so Ed25519 would need an extra crypto dependency (Google Tink) and hand-rolled signing for
no security gain.

## Consequences

- Every reviewer of the deployed app needs a Google account or the mock.
- Access token: ~15 min, held in memory by the SPA. Refresh token: rotating, in an
  httpOnly/Secure/SameSite=Strict (ADR-0011) cookie.
- We run a small token issuer: key management, refresh-token rotation with reuse detection,
  a short access-token TTL as the revocation strategy.

## Tokens, keys, and refresh rotation

- The signing key is a P-256 EC JWK. In production it comes from `pictogram.auth.signing-key`
  (private JWK as JSON); on the `prod` profile a missing key fails startup, since an
  ephemeral key breaks verification across replicas and on restart. Off `prod`, identity
  generates a process-lifetime key and logs a warning.
- Rotation consumes the presented token with a single conditional `UPDATE`, so two refreshes
  racing on the same token can't both succeed. The loser (0 rows updated, or a presented row
  already consumed) is treated as **theft only outside a grace window** (see "Concurrent
  refreshes" below).
- The reuse path does **not** run in the rotation's transaction and there is no `noRollbackFor`.
  `RefreshTokenService` drives its own boundaries with sequential `TransactionTemplate` calls:
  the rotation transaction commits and closes first, then the family revocation runs in its own
  subsequent transaction, then `RefreshTokenReuseException` is thrown. An exception rolls back
  the transaction it flies from, so the revoke has to be already committed by then; and a
  `REQUIRES_NEW` revoke deadlocked — the race loser's failed conditional `UPDATE` holds an
  exclusive tuple lock until its transaction ends, and a second connection revoking the family
  blocked on it forever. Because this only holds with no ambient transaction, `refresh` and
  `signOut` on `IdentityAuthentication` are deliberately **not** `@Transactional` (only
  `authenticate` is), and `RefreshTokenService.rotate` / `revokeFamilyOf` throw
  `IllegalStateException` if a transaction is active rather than trusting that.
- identity contributes the `JwtDecoder` bean the `:app` resource server verifies with, plus
  a published `PictogramAccessTokens` interface for in-process / future-extracted callers.
  A stray `spring.security.oauth2.resourceserver.jwt.*` property (`issuer-uri` /
  `jwk-set-uri`) would arm `OAuth2ResourceServerAutoConfiguration` to contribute a rival
  `JwtDecoder` that holds none of identity's signing material and would reject every
  Pictogram token. Boot's decoder config is `@ConditionalOnMissingBean(JwtDecoder.class)`
  so it already stands down for identity's bean; on top of that identity's decoder is
  `@Primary` and `:app` passes it into the resource-server chain by reference rather than
  relying on a bean-type lookup (#28).
- The transient session that Spring keeps during the OIDC handshake (`/oauth2/**`) holds
  the authorization request across the redirect to Google and back. It is the only
  server-side state, and it is torn down the moment it has served its purpose (see "The
  handshake, its session, and failure paths" below). The Pictogram session (access +
  refresh) stays fully stateless.
- identity's sign-in filter chain matches `/oauth2/**` and `/login/oauth2/**` — the
  redirection endpoint only, not all of `/login/**`. The SPA has its own `/login` route
  (#10); scoping the matcher to `/login/oauth2/**` lets that path fall through to the
  permit-all chain and the SPA fallback (#34) instead of hitting `oauth2Login` / `denyAll`.

### Concurrent refreshes

Treating every second presentation of a consumed refresh token as theft would make a benign
double-submit of a *live* cookie — React StrictMode's double effect, a double-click, a
client retry — revoke the whole family and force the user back to sign-in ~15 minutes
later. Rotation distinguishes the two:

- A presented refresh row that is already consumed (or that loses the `consumeIfLive`
  race) is theft — revoke the family, throw `RefreshTokenReuseException` — **only if it was
  consumed longer than `pictogram.auth.refresh-token-rotation-grace` ago** (default `60s`)
  and the family is not already revoked. Within that window it is a benign concurrent
  refresh: respond `401` with the `session-invalid` Problem Detail and clear the cookie,
  but leave the family intact.
- The racer that won the rotation already set the new cookie, so the SPA's
  silent-refresh-on-`401`-with-one-retry (#10) then succeeds with it. Net cost of a benign
  double-submit: one wasted `401` + one retry, not a logout. The SPA should still
  single-flight its refreshes; the grace window only stops the accidental race from being
  destructive.
- A spent token presented **after** the grace window still revokes the family — reuse
  detection for a genuinely stolen token is unchanged, just delayed by at most the grace
  period.

Re-serving the *same* successor token to both racers for a true `200` on each is out of
scope: it needs the rotation to be idempotent per presented token. If `401`+retry proves
insufficient in practice, that is the fallback.

## The handshake, its session, and failure paths

`SignInCompletion` (`identity/internal/web`) is the **single place** the handshake outcome
becomes an HTTP response — not three Spring hooks each re-deriving part of the decision:

- **`succeeded`** — set the refresh cookie, redirect to the post-login route. This is the
  only writer of the refresh cookie on this chain.
- **`unusable`** — when Google reports `email_verified=false` or omits `email`,
  `GoogleIdentityProvider.verify` throws a typed `UnusableGoogleAccountException`; redirect
  to `pictogram.auth.sign-in-error-redirect` (default `/login?error=sign-in-failed`) with
  the `error` parameter set to the specific reason (`email-unverified` / `email-missing`) so
  the SPA shows a tailored message. No Pictogram session is issued.
- **`failed`** — a declined consent, state mismatch, or token-exchange error redirects to
  the same route with the generic `error=sign-in-failed`.

Every path ends the handshake servlet session exactly once: after any cookie is set and
before the redirect, `HttpSession#invalidate` (null-safe) runs and the
`SecurityContextHolder` is cleared. Otherwise the `JSESSIONID` Spring created to hold the
authorization request would linger authenticated until timeout — `AuthController.logout` is
on the stateless chain and never touches it. Anything else thrown from the `/oauth2` chain
(no `DispatcherServlet` behind it, so the shared `ApiExceptionHandler` never sees it) is
rendered `application/problem+json` (`internal-error`, 500) by `OidcChainErrorFilter`, never
a white-label page. `OidcSignInSuccessHandler`, `SignInFailureHandler`, and
`OidcChainErrorFilter` are one-line delegations to `SignInCompletion`.
