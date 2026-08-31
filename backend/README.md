# Pictogram backend

A modular monolith: one Spring Boot deployable (`:app`), one Gradle subproject per bounded
context, boundaries enforced by Spring Modulith.

The Gradle build lives in this directory — `frontend/` is a separate build. Run every
command below from `backend/` (`cd backend` first).

```
shared-kernel   whitelisted shared module — ID value types only (UserId, PostId, MediaId)
identity        profile        media        post
follow          feed           engagement
app             the deployable — depends on every context
test-support    test-only helpers (not a bounded context)
```

Each context's API is `me.imshy.pictogram.<context>`; everything under
`…<context>.internal.{domain,application,infrastructure,web}` is hidden by Modulith. The
`internal.*` packages are created as code lands — only the exported `package-info` (with
`@ApplicationModule`) exists in the skeleton.

## Toolchain

- JDK 25, provisioned automatically by the Gradle toolchain (foojay resolver). Only a JVM
  to *run* Gradle is required on the machine.
- Docker, for the Testcontainers-backed module tests.

## Common tasks

Run these from `backend/`.

Compile, all tests, and the module-boundary check:

```
./gradlew build
```

Just the boundary check:

```
./gradlew :app:test --tests ModulithStructureTest
```

Run the app on the `local` profile (starts infra from `compose.dev.yaml`):

```
./gradlew :app:bootRun
```

## IntelliJ

The committed run configs assume the **repo root** is open as the project (so
`$PROJECT_DIR$` is the repo root and the configs point at `$PROJECT_DIR$/backend`).

1. Open the repo root. Link the Gradle build if IntelliJ doesn't prompt: open
   `backend/settings.gradle.kts` → **Link Gradle Project** (or **+** in the Gradle tool
   window → pick `backend`).
2. **Settings → Build, Execution, Deployment → Build Tools → Gradle**:
   - *Distribution* / "Use Gradle from" → **`gradle-wrapper.properties`** (this repo needs
     Gradle 9.7.1; IntelliJ's bundled Gradle is older and won't import under JDK 26).
   - *Gradle JVM* → any installed JDK 21–26 (e.g. the Homebrew `openjdk@25`). A stale entry
     here is the other common "can't import" cause.
3. **Reload All Gradle Projects** (⟳). When it's green the modules show as
   `pictogram.<subproject>.main` / `.test`.

Run configs under `.run/` (repo root):

| Config | Notes |
|---|---|
| **All Tests** | Gradle `test` |
| **Modulith Verify** | just `ModulithStructureTest` |
| **Pictogram App (Gradle bootRun)** | runs via Gradle — any IntelliJ edition |
| **Pictogram App (local)** | Spring Boot config — **Ultimate only**; working dir is the repo root so `compose.dev.yaml` resolves |

## Tests

- **Domain units** — pure, no Spring (`shared-kernel`).
- **Module-integration** — the bulk. Extend
  `me.imshy.pictogram.testsupport.ModuleIntegrationTest` (from `:test-support`): a sliced
  `@ApplicationModuleTest` against one **singleton** Testcontainers PostgreSQL
  (`SharedPostgres`, `withReuse` locally). Isolation is by `TRUNCATE` after each test, not
  transaction rollback (ADR-0007).
- **Application smoke** — `ApplicationSmokeTest` boots the whole app and checks the health
  probes.

For container reuse across local runs, put `testcontainers.reuse.enable=true` in
`~/.testcontainers.properties`.

## OpenAPI

springdoc publishes the live document as JSON at `/v3/api-docs` (no Swagger UI on the
classpath). `backend/openapi.json` is the committed copy the frontend generates its typed
client from:

```
./gradlew :app:generateOpenApiSpec   # boots the app, rewrites backend/openapi.json
```

`OpenApiContractTest` runs in every `./gradlew build` and fails when the committed file
drifts from what the app produces — regenerate and commit when you change an endpoint.
`OpenApiConfiguration` pins the title/version and a relative server URL so the file only
moves when the API does.

The document currently reflects springdoc's inference from method signatures: every
operation is documented as `200`, and `ResponseEntity<?>` returns show as an untyped
object. Precise status codes (201/204), response schemas, and error responses come as each
endpoint's own slice annotates its web contract (the auth client with #10, profile reads
with #11).

## Observability

Actuator health/liveness/readiness under `/actuator`; Spring Modulith observability
(module-boundary spans) is on via `spring-modulith-observability` + a Brave tracer bridge.
Structured ECS-JSON console logging is on by default (plain text on the `test` profile).

## Database migrations

Flyway runs on startup. Each module owns its schema and ships its own migrations under
`<module>/src/main/resources/db/migration/`, version-prefixed by a module ordinal
(`V1_…` identity, `V2_…` profile, …) so they share one ordered history — see
[ADR-0009](../docs/adr/0009-flyway-migrations-per-module.md).

## CI

`.github/workflows/ci.yml` runs `./gradlew build` on every PR — Modulith `verify()`, the
unit + `@ApplicationModuleTest` + in-process `@Tag("fast")` scenario suites, and the
`openapi.json` drift check. `@Tag("blackbox")` tests (the `ContainerDriver` over the built
image) are excluded by default and run only on `main`, selected with `-PincludeBlackbox`.

## Not in this skeleton

Security and the first schema landed with the identity slice (#8); the OpenAPI export with
#6; CI with #7.
