# TDD, and scenario tests behind an in-process / container driver

- **Status:** Accepted
- **Relates to:** ADR-0001 (the test-first mandate this ADR implements)

All of Pictogram is built **test-first**. The test shape is a diamond: a thin layer of
domain unit tests for edge cases, the **bulk at module-integration level**
(`@ApplicationModuleTest` against a singleton Testcontainers PostgreSQL, `withReuse` locally),
and a thin end-to-end cap.

User-goal scenarios are written **once** against a `PictogramApi` driver interface with
intention-revealing actions (`registerViaGoogle()`, `chooseUsername()`, `publishPost()`,
`follow()`, `openFeed()`). Each acceptance journey is one `*Scenarios` interface
(`FollowScenarios`, `FeedScenarios`, …); two aggregators implement the whole list and run it
against two transports:

- **`InProcessDriver`** — `@SpringBootTest` plus Testcontainers Postgres / MinIO /
  `mock-oauth2-server`. Run by `InProcessScenarioTest`, tagged `fast`, on every build. Split
  two ways (#51): a deep `HttpPictogramApi` owns all request/response mapping against a base
  URI, and a small `SignIn` port owns "how a session is minted". `InProcessDriver` composes
  one `HttpPictogramApi` — that base URI plus a `mock-oauth2-server` `SignIn` — and delegates
  to it.
- **`ContainerDriver`** — the actual application image over HTTP, treated as the truth of
  what production does. Run by `BlackboxScenarioTest`, tagged `blackbox`. It composes the
  same `HttpPictogramApi` with a compose URI and an `InteractiveLoginSignIn` that walks
  `mock-oauth2-server`'s interactive form. The container's Postgres is never truncated, so
  each instance carries a per-run namespace token: it prefixes the scenarios' hard-coded
  emails itself and hands the token to a `UsernamePolicy`, injected into `HttpPictogramApi`
  in the same shape as `SignIn` — identity for `InProcessDriver`, prefix-and-strip for
  `ContainerDriver` — so the scenario bodies stay transport-blind. A divergence between the
  two transports is a bug in one of them. (`NamespacedActor`, a full-`Actor`-interface
  decorator, did this same job first; it was a deliberate stopgap, not a design to keep, and
  #154 retired it once the `SignIn`-shaped seam was there to replace it with.)
  `OrphanMediaCollectionScenarioTest` has no container twin — it drives a scheduled sweep and
  a property, neither HTTP-observable.

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
- Flakiness is treated as a defect, not a retry target: `retries: 0` in
  `playwright.config.ts`, no Gradle retry.
- CI runs the `blackbox` job on the push to `main` only — the PR gate already ran the fast
  suites on the identical up-to-date tree, and building the image + booting the stack is too
  slow for every PR. A red `blackbox` run is therefore a **fix-forward signal on `main`**,
  not an automatic merge block: making it one needs branch protection (require the
  `Blackbox` check + "require branches up to date"), which this repo's plan does not offer.
  `-PincludeBlackbox` opts the test task out of state tracking so it never reports a cached
  pass against a stale stack.
