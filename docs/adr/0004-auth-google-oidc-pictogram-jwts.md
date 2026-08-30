# Authentication: Google-only OIDC, backend-driven, Pictogram-issued JWTs

Users authenticate **only via Google** in v1, using the OIDC Authorization Code flow with
PKCE handled **server-side** by Spring Security's OAuth2 Client — the browser just follows a
"Continue with Google" link and the client secret never leaves the backend. After verifying
the Google identity, the `identity` module mints **Pictogram's own** access and refresh
JWTs, signed with **EdDSA (Ed25519)**; no other module ever sees a Google token. An
`IdentityProvider` seam in `identity` allows adding email/password later.

Chosen because it removes all password-handling surface (hashing, resets, verification,
lockout) from v1; because stateless Pictogram tokens verified with a public key fit the
service-extraction goal in ADR-0001; and because a future extracted service can verify
tokens with the public key while holding no signing material. `mock-oauth2-server` stands
in for Google in tests.

## Consequences

- Every reviewer of the deployed app needs a Google account or the mock.
- Access token: ~15 min, held in memory by the SPA. Refresh token: rotating, in an
  httpOnly/Secure/SameSite=Lax cookie.
- We run a small token issuer: key management, refresh-token rotation with reuse detection,
  a short access-token TTL as the revocation strategy.

## Amendment (#8): ES256, not Ed25519

The access token is signed with **ES256 (ECDSA on P-256)**, not EdDSA/Ed25519 as first
written. Both are asymmetric, so every property this ADR relies on holds unchanged — a
service extracted from the monolith verifies with the public key and holds no signing
material. ES256 is chosen only for tooling: Spring Security 7's `NimbusJwtEncoder` still
rejects EdDSA ([spring-security#17098](https://github.com/spring-projects/spring-security/issues/17098)),
so Ed25519 would need an extra crypto dependency (Google Tink) and hand-rolled signing for
no security gain.

- The signing key is a P-256 EC JWK. In production it comes from `pictogram.auth.signing-key`
  (private JWK as JSON); unset, identity generates a process-lifetime key and logs a warning.
- Rotation consumes the presented token with a single conditional `UPDATE`, so two refreshes
  racing on the same token can't both succeed — the loser (0 rows updated) is treated as reuse
  and the family is revoked. The SPA must single-flight its refresh calls.
- The reuse path revokes the family **in the rotation's own transaction** and throws a
  `RefreshTokenReuseException` marked `noRollbackFor`, so the revoke commits as the exception
  unwinds. Isolating the revoke in a `REQUIRES_NEW` transaction deadlocked: the race loser's
  failed conditional `UPDATE` still holds an exclusive tuple lock until its transaction ends,
  and a second connection revoking the family blocked on it forever.
- identity contributes the `JwtDecoder` bean the `:app` resource server verifies with, plus
  a published `PictogramAccessTokens` interface for in-process / future-extracted callers.
- The transient session that Spring keeps during the OIDC handshake (`/oauth2/**`) is the
  only server-side state; the Pictogram session (access + refresh) stays fully stateless.
