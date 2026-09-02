# Database schema: Flyway, one schema per module, one global migration sequence

- **Status:** Accepted
- **Relates to:** ADR-0002 (schema per module, no cross-schema FKs), ADR-0006 (media keeps its row here, bytes in MinIO)

Each bounded context owns its own PostgreSQL **schema** and never a foreign key across a
schema boundary (ADR-0002 — contexts reference each other by ID value only). Schema changes
are applied with **Flyway**, run on startup by the `:app` process against the one database.

Migrations live in the module that owns the schema: `backend/<module>/src/main/resources/db/migration/`.
Flyway merges these from every module's jar into the single default location
`classpath:db/migration`, sharing one `flyway_schema_history` table.

Flyway needs a **globally unique, ordered version** per migration. That version is a single
**flat, zero-padded 3-digit sequence** shared across all modules — `V001`, `V002`, …
`V999` — regardless of which module the file lives in:

| version | file | owning module |
|---|---|---|
| `V001` | `identity/…/db/migration/V001__create_identity_schema.sql` | identity |
| `V002` | `profile/…/db/migration/V002__create_profile_schema.sql` | profile |
| `V003` | `media/…/db/migration/V003__create_media_schema.sql` | media |
| `V004` | `post/…/db/migration/V004__create_post_schema.sql` | post |

The owning module is identified by the file's **path** and the migration **description**,
not by the version number. A new migration takes the next free number in the sequence, so
it always sorts after everything already applied.

Each migration does `create schema if not exists <module>;` and qualifies every object with
that schema. A module with no persistence ships no migrations.

Two alternatives were rejected:

- **A Flyway location and history table per module.** Spring Boot auto-configures a single
  `Flyway` bean against the single `DataSource`, and per-schema histories would mean either
  multiple `Flyway` beans and `DataSource`s or a custom callback — a lot of machinery for a
  monolith whose modules already deploy together.
- **A module-ordinal version prefix** (`identity`→`V1_`, `profile`→`V2_`, …, each module
  running its own `_NNN` sub-sequence). It stays globally monotonic **only while modules
  gain migrations in ordinal order**. The moment an already-integrated lower module needs a
  *new* migration, its version sorts *before* higher modules' migrations that are already
  applied, and Flyway's default `validateOnMigrate` aborts startup on any non-fresh database
  (`Detected resolved migration not applied to database: 3.002`). The flat sequence has no
  such failure mode — the next number is always the highest — so validation stays on.

## Consequences

- Only the module that declares the `spring-boot-starter-flyway` dependency contributes
  migrations. Adding persistence to another module means adding that dependency there and a
  `V<NNN>__…` file with the next free number.
- **Accepted downside**: two concurrent branches can both pick the same next number.
  `V005__a.sql` and `V005__b.sql` are different paths, so git merges them with no conflict
  marker — the collision is silent until the next startup after both land, where Flyway
  fails with a duplicate-version error. It is a runtime footgun, not a merge-time one.
  Judged acceptable at current scale; the timestamp-prefix variant (`V20260901__…`) is the
  fallback if it becomes frequent.
- `@ApplicationModuleTest` for a module with migrations runs Flyway against the shared
  Testcontainers Postgres; `DatabaseCleaner` truncates the data but leaves the schema and
  `flyway_schema_history` in place between tests.
- A service later extracted from the monolith takes its module's `db/migration` files and
  its schema with it; nothing else referenced them.
