# TDD, and scenario tests behind an in-process / container driver

All of Pictogram is built **test-first**. The test shape is a diamond: a thin layer of
domain unit tests for edge cases, the **bulk at module-integration level**
(`@ApplicationModuleTest` against a singleton Testcontainers PostgreSQL, `withReuse` locally),
and a thin end-to-end cap.

User-goal scenarios are written **once** against a `PictogramApi` driver interface with
intention-revealing actions (`registerViaGoogle()`, `chooseUsername()`, `publishPost()`,
`follow()`, `openFeed()`), and run against two transports:

- `InProcessDriver` — `@SpringBootTest` plus Testcontainers Postgres / MinIO /
  `mock-oauth2-server`. Tagged `fast`, runs every build. Split two ways (#51): a deep
  `HttpPictogramApi` owns all request/response mapping against a base URI, and a small
  `SignIn` port owns "how a session is minted". `InProcessDriver` composes one
  `HttpPictogramApi` — that base URI plus a `mock-oauth2-server` `SignIn` — and delegates
  to it. `ContainerDriver` (#20) composes the same class with a compose URI and a
  real-handshake `SignIn` — a config, not a second mapping layer.
- `ContainerDriver` — the actual application image over HTTP. Tagged `blackbox`, runs in
  CI. This is treated as the truth of what production does.

## Consequences

- Assertions on asynchronous outcomes use **Awaitility**, never `Thread.sleep`; time is an
  injectable `Clock`; test data is randomised per test; isolation is by table truncation,
  not transaction rollback (which breaks across async listeners).
- There is **one** `Clock` bean, not a per-module `@ConditionalOnMissingBean` fallback:
  `:app`'s `ClockConfiguration` for the running application and `@SpringBootTest`s, and
  `test-support`'s `ModulithTestApplication` for `@ApplicationModuleTest` (which boots
  without `:app` and cannot import the bean — spring-modulith#1381). A controlled-time test
  overrides it by type with `@MockitoBean Clock`.
- Tests are named as user goals with Given/When/Then bodies; Java uses a fluent DSL +
  AssertJ, the browser layer uses a page-object/actor layer + Playwright's `expect`.
- Flakiness is treated as a defect, not a retry target.
