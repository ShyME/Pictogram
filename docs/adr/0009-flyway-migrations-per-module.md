# Database schema: Flyway, one schema per module, version-namespaced migrations

Each bounded context owns its own PostgreSQL **schema** and never a foreign key across a
schema boundary (ADR-0002 — contexts reference each other by ID value only). Schema changes
are applied with **Flyway**, run on startup by the `:app` process against the one database.

Migrations live in the module that owns the schema: `backend/<module>/src/main/resources/db/migration/`.
Flyway merges these from every module's jar into the single default location
`classpath:db/migration`, sharing one `flyway_schema_history` table. Because Flyway needs a
**globally unique, ordered version** per migration, the first version segment is a fixed
**module ordinal** and the rest is that module's own sequence:

| module | prefix | example |
|---|---|---|
| identity | `V1_` | `V1_001__create_identity_schema.sql` |
| profile | `V2_` | `V2_001__…` |
| media | `V3_` | … |
| post | `V4_` | … |
| follow | `V5_` | … |
| feed | `V6_` | … |
| engagement | `V7_` | … |

Each migration does `create schema if not exists <module>;` and qualifies every object with
that schema. A module with no persistence ships no migrations.

The alternative — a Flyway location and history table per module — was rejected: Spring Boot
auto-configures a single `Flyway` bean against the single `DataSource`, and per-schema
histories would mean either multiple `Flyway` beans and `DataSource`s or a custom callback,
which is a lot of machinery for a monolith whose modules already deploy together.

## Consequences

- Only the module that declares the `spring-boot-starter-flyway` dependency contributes
  migrations; `identity` is the first. Adding persistence to another module means adding that
  dependency there and a `V<ordinal>_…` file.
- `@ApplicationModuleTest` for a module with migrations runs Flyway against the shared
  Testcontainers Postgres; `DatabaseCleaner` truncates the data but leaves the schema and
  `flyway_schema_history` in place between tests.
- A service later extracted from the monolith takes its module's `db/migration` files and
  its schema with it; nothing else referenced them.
