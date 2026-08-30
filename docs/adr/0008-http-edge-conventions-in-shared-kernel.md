# HTTP edge conventions live in shared-kernel

Every module owns its own REST resources under `internal.web` (ADR-0005, no BFF), so the
cross-cutting HTTP shapes — RFC 9457 Problem Details, the `{ items, nextCursor }`
pagination envelope with its opaque keyset cursor, and resolving the access token into an
explicit `UserId` / `ViewerId` at the edge — must sit somewhere **every** module's web
layer can depend on. Spring Modulith allows that for exactly one place: the whitelisted
`shared` module.

The v1 spec scoped `shared-kernel` to "ID value types only — no behaviour, entities, or
DTOs". This ADR **amends that**: `shared-kernel` also carries the HTTP edge conventions, in
the `me.imshy.pictogram.shared.http` package, exposed as the Modulith named interface
`http`. It stays a thin technical kernel — no domain logic, no persistence — and the web
dependencies it now pulls (`spring-webmvc`, `spring-security-oauth2-resource-server`,
Jackson) are compile-time only; the servlet container stays in `:app`.

The alternative — a separate `:api-conventions` Gradle subproject and Modulith shared
module — was rejected as more scaffolding (a second `sharedModules` entry, its own
`package-info`, build file) for no isolation benefit over a sub-package here.

## Consequences

- `shared-kernel` depends on Spring (it was previously pure Java). The dependency is the
  web/security API surface only; it does not start a web server in a module slice test.
- The concrete `SecurityFilterChain` and the JWT `JwtDecoder` stay in `:app` — composition,
  not convention. Until the identity slice (#8) mints Pictogram access tokens and wires
  their EdDSA verification key, `:app` installs a decoder that rejects every token, so
  `/api/**` is uniformly 401.
- A later screen needing heavy cross-context fan-in is still handled by a `bff` module
  (ADR-0005), not by growing `shared-kernel`.
