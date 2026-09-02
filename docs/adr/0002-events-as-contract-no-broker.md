# Cross-module integration: events as contract, published-interface queries, no broker

- **Status:** Accepted
- **Relates to:** ADR-0003 (feed uses the query path), ADR-0009 (schema per module, no cross-schema FKs)

Modules must stay decoupled, but v1 has almost no genuine cross-context reactions. We are
integrating modules two ways: **Spring Modulith application events** for state-change
notifications (each module's forward-facing public contract), and **synchronous calls to
another module's published interface for queries only** (e.g. `feed` asking `follow` for a
viewer's followees). No external message broker in v1 — Spring Modulith's event
externalisation is the documented path when a module is later extracted.

The **Event Publication Registry** (persisted, retried events) is deferred until there is
an async consumer whose loss actually matters. v1's only cross-module reaction — creating a
`Profile` for a new `User` — is instead handled synchronously by the onboarding step, so a
missing profile simply means "not yet onboarded", not a lost event.

## Consequences

- Events (`UserRegistered`, `PostPublished`, `UserFollowed`, …) are emitted from day one
  even with zero consumers, so fan-out-on-write, notifications, and comment counts can be
  added later without touching the producing modules.
- No listener may mutate another module's state; that is what published-interface commands
  would be for. The only one so far is `media`'s `OrphanCollection`, driven by a `@Scheduled`
  trigger at the composition root — infrastructure, not a cross-context reaction.
- A published-interface query may be declared by the module that *needs* the answer when the
  Gradle dependency only runs one way: `media` owns the `PostReferences` port and `post`
  supplies the adapter, so the arrow stays `post → media` (orphan collection, #16).
