# Pictogram backend

A modular monolith: one Spring Boot deployable (`:app`), one Gradle subproject per bounded context, boundaries enforced
by Spring Modulith. All five contexts — `identity`, `profile`,
`media`, `post`, `social` — are built, each with `@ApplicationModuleTest` coverage and a user-goal scenario suite run
both in-process and black-box (see
[`CONTEXT-MAP.md`](../CONTEXT-MAP.md) and the Tests section below). `social` holds the follow graph, feed and likes as
three `internal/` sub-domains (#128 merged what were three finer-grained contexts).

The Gradle build lives in this directory — `frontend/` is a separate build. Run every command below from `backend/`
(`cd backend` first).

```
shared-kernel   whitelisted shared module — ID value types (UserId, PostId, MediaId,
                ViewerId) plus the shared HTTP edge (ADR-0008)
identity        profile        media        post        social
app             the deployable — depends on every context
test-support    test-only helpers (not a bounded context)
```

Each context's API is `me.imshy.pictogram.<context>`; everything under
`…<context>.internal` is hidden by Modulith. `internal` is flat by default — a subpackage appears only for the HTTP edge
(`internal.web`) or for a genuine cluster inside a crowded module (files you would extract, move, or delete as a unit),
named for the idea not the layer: `identity.internal.refreshtoken`, `identity.internal.accesstoken`,
`social.internal.follow` / `feed` / `likes`. Composition happens in the `internal`
root — where `social`'s in-module `FollowGraph` seam sits, so `feed` can read the follow graph without a slice reaching
sideways. `ModuleInternalSlicingTest` fails the build if one subpackage reaches sideways into a sibling — Modulith's
`internal` rule, one level down. Splitting a cluster out costs some `package-private → public`; that's the trade for a
scannable package, and the slice test is what keeps the widened surface from being abused.

## Toolchain

- JDK 25, provisioned automatically by the Gradle toolchain (foojay resolver). Only a JVM to *run* Gradle is required on
  the machine.
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
   `backend/settings.gradle.kts` → **Link Gradle Project** (or **+** in the Gradle tool window → pick `backend`).
2. **Settings → Build, Execution, Deployment → Build Tools → Gradle**:
    - *Distribution* / "Use Gradle from" → **`gradle-wrapper.properties`** (this repo needs Gradle 9.7.1; IntelliJ's
      bundled Gradle is older and won't import under JDK 25).
    - *Gradle JVM* → JDK 25 (e.g. the Homebrew `openjdk@25`). A stale entry here is the other common "can't import"
      cause.
3. **Reload All Gradle Projects** (⟳). When it's green the modules show as
   `pictogram.<subproject>.main` / `.test`.

Run configs under `.run/` (repo root):

| Config                             | Notes                                                                                               |
|------------------------------------|-----------------------------------------------------------------------------------------------------|
| **All Tests**                      | Gradle `test`                                                                                       |
| **Modulith Verify**                | just `ModulithStructureTest`                                                                        |
| **Pictogram App (Gradle bootRun)** | runs via Gradle — any IntelliJ edition                                                              |
| **Pictogram App (local)**          | Spring Boot config — **Ultimate only**; working dir is the repo root so `compose.dev.yaml` resolves |

## Tests

- **Domain units** — pure, no Spring (`shared-kernel`).
- **Module-integration** — the bulk. Extend
  `me.imshy.pictogram.testsupport.ModuleIntegrationTest` (from `:test-support`): a sliced
  `@ApplicationModuleTest` against one **singleton** Testcontainers PostgreSQL (`SharedPostgres`, `withReuse` locally).
  Isolation is by `TRUNCATE` after each test, not transaction rollback (ADR-0007). `media` tests also wire the singleton
  MinIO (`SharedMinio`) for the object store.
- **Application smoke** — `AppSmokeTest` boots the whole app and checks the health probes.
- **Scenarios** — user-goal journeys in `app` test `scenario/`, each written once as a
  `*Scenarios` mixin against the `PictogramApi` interface and run through two transports (ADR-0007): `InProcessTests`
  (`@Tag("fast")`, full boot + `InProcessDriver`, every build) and `RunningStackAppTests` (`@Tag("blackbox")`,
  `ContainerDriver` over the built image, `main` only — see below). `OrphanMediaCollectionTest` is in-process only (it
  drives a scheduled job, not an HTTP endpoint).

For container reuse across local runs, put `testcontainers.reuse.enable=true` in
`~/.testcontainers.properties`.

### Blackbox scenarios locally

`./gradlew build -PincludeBlackbox` swaps the tag filter to `@Tag("blackbox")` and runs
`RunningStackAppTests` against `PICTOGRAM_BASE_URL` (default `http://localhost:8080`). It needs the container stack up —
`task test:blackbox` from the repo root builds it, runs the backend blackbox scenarios and the Playwright journeys, and
tears it down; or point it at a
`task up` stack yourself. See the `scenario` package docs for how it stays isolated on the un-truncated container DB.

## OpenAPI

springdoc publishes the live document as JSON at `/v3/api-docs` (no Swagger UI on the classpath). `backend/openapi.json`
is the committed copy the frontend generates its typed client from:

```
./gradlew :app:generateOpenApiSpec   # boots the app, rewrites backend/openapi.json
```

`OpenApiContractTest` runs in every `./gradlew build` and fails when the committed file drifts from what the app
produces — regenerate and commit when you change an endpoint.
`OpenApiConfiguration` pins the title/version and a relative server URL so the file only moves when the API does.

The document currently reflects springdoc's inference from method signatures: every operation is documented as `200`,
and `ResponseEntity<?>` returns show as an untyped object. Precise status codes (201/204), response schemas, and error
responses come as each endpoint's own slice annotates its web contract (the auth client with #10, profile reads with
#11).

## Observability

Actuator health/liveness/readiness under `/actuator`; Spring Modulith observability (module-boundary spans) is on via
`spring-modulith-observability` + a Brave tracer bridge. Structured ECS-JSON console logging is on by default (plain
text on the `test` profile).

## Database migrations

Flyway runs on startup. Each module ships its schema's migrations under
`<module>/src/main/resources/db/migration/` (`social` ships one set per sub-domain schema,
`follow` and `likes`). Version numbers are a single flat sequence shared across every module — `V001__…`, `V002__…`, …
`V999__…`, zero-padded to three digits — so they merge into one ordered `flyway_schema_history`. The owning module is
told by the file's path and the migration description, not by the number. A new migration always takes the next free
number, so it always sorts last and applies cleanly with validation on. See
[ADR-0009](../docs/adr/0009-flyway-migrations-per-module.md).

The compose stacks and `SharedPostgres` run `postgres:18-alpine`; the named `pgdata` volume mounts at
`/var/lib/postgresql` (Postgres 18 keeps `PGDATA` in a version-specific subdirectory below that). A fresh `task up`
provisions a PG18-format volume — nothing to migrate.

## CI

`.github/workflows/ci.yml` runs `./gradlew build` on every PR — Modulith `verify()`, the unit +
`@ApplicationModuleTest` + in-process `@Tag("fast")` scenario suites, and the
`openapi.json` drift check. `@Tag("blackbox")` tests (the `ContainerDriver` over the built image) are excluded by
default and run only on `main`, selected with `-PincludeBlackbox`.
