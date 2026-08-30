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
