# TDD, and scenario tests behind an in-process / container driver

- **Status:** Accepted; amended (see Change log)
- **Amended by:** [#20](https://github.com/ShyME/pictogram/issues/20)
- **Relates to:** ADR-0001 (the test-first mandate this ADR implements)

## Change log

| Issue | Change |
|---|---|
| [#20](https://github.com/ShyME/pictogram/issues/20) | The container transport landed. Each journey is one `*Scenarios` interface against `PictogramApi`; `InProcessScenarioTest` (`@Tag("fast")`) and `BlackboxScenarioTest` (`@Tag("blackbox")`, `ContainerDriver` over the built image) run the whole list. The `blackbox` job runs on the push to `main` only and is a fix-forward signal, not a merge block. Detail in the amendment below. |

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

## Amendment (#20): the container transport landed

Each acceptance journey is one `*Scenarios` test interface (`FollowScenarios`,
`FeedScenarios`, …) written against `PictogramApi`. Two aggregators implement the whole
list: `InProcessScenarioTest` (`@Tag("fast")`, `InProcessDriver`) and `BlackboxScenarioTest`
(`@Tag("blackbox")`, `ContainerDriver`). A divergence between the two is a bug in one
transport. `OrphanMediaCollectionScenarioTest` has no container twin — it drives a scheduled
sweep and a property, neither HTTP-observable.

`ContainerDriver` came out slightly heavier than "a config": `HttpPictogramApi` still owns
all request/response mapping and `InteractiveLoginSignIn` walks `mock-oauth2-server`'s
interactive form, but the container's Postgres is never truncated, so each instance (one per
test) namespaces the scenarios' hard-coded emails and usernames with a per-run token and
strips it from returned profiles. The scenario bodies stay transport-blind.

CI runs the `blackbox` job on the push to `main` only — the PR gate already ran the fast
suites on the identical up-to-date tree, and the job builds the image + boots the stack,
too slow for every PR. So a red `blackbox` run is a **fix-forward signal on `main`**, not an
automatic merge block: turning it into one needs branch protection (require the `Blackbox`
check + "require branches up to date"), which this repo's plan doesn't offer. `-PincludeBlackbox`
opts the test task out of state tracking so it never reports a cached pass against a stale
stack; `retries: 0` in `playwright.config.ts` and no Gradle retry keep the flake budget at
zero.
