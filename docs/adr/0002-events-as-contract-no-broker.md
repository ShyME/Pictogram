# Cross-module integration: events as contract, published-interface queries, no broker

- **Status:** Accepted
- **Relates to:** ADR-0003 (feed uses the query path), ADR-0009 (schema per module, no
  cross-schema FKs), ADR-0014 (chat's network-boundary integration — a different scope,
  not an exception to this ADR)

Modules must stay decoupled, but v1 has almost no genuine cross-context reactions. We are
integrating modules two ways: **Spring Modulith application events** for state-change
notifications (each module's forward-facing public contract), and **synchronous calls to
another module's published interface for queries only** (e.g. `social`'s feed asking
`post` for a keyset page of an author's posts). No external message broker in v1 — Spring Modulith's event
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
  would be for. A listener reacting to another module's event by changing **its own** state
  is fine: `social`'s `comment` sub-domain consumes `post`'s `PostDeleted` to hard-delete
  that post's thread (#138). It is a plain `@EventListener`, synchronous in the deleting
  transaction — the registry stays deferred because a lost cleanup only leaves harmless
  orphan comment rows, not a broken invariant. `media`'s `OrphanCollection` is the other
  cross-module reaction, driven by a `@Scheduled` trigger at the composition root.
- A published-interface query may be declared by the module that *needs* the answer when the
  Gradle dependency only runs one way: `media` owns the `PostReferences` port and `post`
  supplies the adapter, so the arrow stays `post → media` (orphan collection, #16).
- This ADR governs integration **inside** the monolith. `chat` (ADR-0014) is a separate
  deployable and reaches it only by verifying a JWT — a network boundary between two
  services, not a broker, and outside this ADR's scope.
