# Pictogram — Context Map

Pictogram is a small Instagram-like web app: people upload square image posts, follow
other people, and scroll a feed of posts from the people they follow. It is a portfolio
project built to practise **Domain-Driven Design** and **Test-Driven Development**, as a
modular monolith whose module boundaries are drawn so they could later be extracted into
separate services.

Each module below is one bounded context, one Gradle subproject, and one Spring Modulith
module — except `chat`, a separate service that never enters this deployable at all
(ADR-0014). `social` carries four internal sub-domains (`follow`, `feed`, `likes`, `comment`) —
the first three were separate contexts in early v1 — kept apart by an `internal/` package
each and the `InternalSlicingTest` guard. `shared-kernel` is the single exception: a whitelisted module every context may
depend on. It holds the ID value types (`UserId`, `PostId`, `MediaId`, `ViewerId`) and,
in its `http` sub-package, the cross-cutting HTTP edge conventions — Problem Details, the
pagination envelope, current-user resolution (ADR-0008). No domain behaviour, entities, or
persistence.

## Contexts

- [identity](./backend/identity/CONTEXT.md): authenticates people via Google and issues Pictogram's own tokens.
- [profile](./backend/profile/CONTEXT.md): the public face of a user — username, display name, bio — and the onboarding step that creates it.
- [media](./backend/media/CONTEXT.md): stores uploaded images as bytes, re-encoded to one canonical square format.
- [post](./backend/post/CONTEXT.md): the `Post` — one image plus an optional caption, published by an author.
- [social](./backend/social/CONTEXT.md): the follow graph, the feed assembled from it, and the likes and comments on a post — one context over four internal sub-domains (`follow`, `feed`, `likes`, `comment`).
- [chat](./chat/CONTEXT.md): live 1:1 messaging between two users — not a module of this deployable; a separate service (ADR-0014).
- [notifications](./backend/notifications/CONTEXT.md): the in-app notification list, fed by the one Kafka-externalised flow out of `social`. The broker and JPA outbox are wired (#195), `social` publishes to `pictogram.social` (#196), and the module consumes it and stores notifications (#197). The read API and SPA bell are still to come (#198–#199).

## Relationships

- **All contexts → `shared-kernel`**: depend on it for ID value types (except `chat`, whose separate Gradle build can't declare that dependency at all — see below). Contexts reference each other's data **by ID only** — never object references, never foreign keys across schemas.
- **identity → profile**: `identity` emits `UserRegistered` when a person first authenticates. In v1 the `Profile` is not created from that event — it is created by the **onboarding** step when the person picks a username. A user without a profile is a legitimate "not yet onboarded" state.
- **social (feed) → post**: `social`'s feed sub-domain calls `post`'s published interface (`PublishedPosts`) synchronously to assemble a page (fan-out-on-read), behind its own `FeedQuery` port (ADR-0003). The other half of the fan-out — the follow graph — is now an in-module call (`FollowGraph` in `social.internal`). The feed has no store.
- **post → media**: a `Post` holds a `MediaId`, and checks ownership via `MediaCatalog` on publish. `media`'s orphan collection needs to know which media a post still references; since the Gradle arrow only runs this way, `media` declares that as a port (`PostReferences`) and `post` provides the adapter.
- **social → post**: a `Like` and a `Comment` each hold a `PostId`. `social`'s `comment` sub-domain consumes `post`'s `PostDeleted` to hard-delete that post's thread (the one real cross-context event reaction in v1 — synchronous, no registry, ADR-0002), and calls `PublishedPosts.authorOf` to check comment-delete permission. `PostPublished` still has no consumer.
- **Events emitted, mostly unconsumed in v1**: `UserRegistered`, `UserFollowed`, `UserUnfollowed`, `PostPublished`, `PostDeleted`, `PostLiked`, `PostUnliked`, `PostCommented`, `CommentDeleted`, `ProfileUpdated`. They exist as each module's public contract so consumers (fan-out-on-write feed, notifications) can be added later without touching producers. `PostDeleted` now has two in-process consumers — `social`'s comment-thread cleanup and `notifications`' purge of a deleted post's notifications.
- **social → notifications (Kafka, ADR-0015)**: the one flow that leaves the JVM. `social` `@Externalized`s `PostLiked` / `PostCommented` / `UserFollowed` onto the single `pictogram.social` topic as a `SocialEvent` JSON payload keyed by recipient id (#196), relayed from the JPA event-publication registry that acts as a transactional outbox. `notifications` consumes it with a hand-written `spring-kafka` `@KafkaListener` (consumer group `notifications`, `concurrency=3`, manual ack post-commit, retry → `pictogram.social.DLT`), skips self-actions, dedups redeliveries on a natural key, and stores one notification per event (#197). It also consumes `post`'s `PostDeleted` in-process to purge that post's notifications. This is the only externalised flow — all other integration stays synchronous published-interface queries and in-process events (ADR-0002, which ADR-0015 narrows, not supersedes). Undo events are neither externalised nor consumed.
- **chat ↔ everything else**: no data dependency in either direction. It verifies Pictogram JWTs (ADR-0004) and knows a caller only by the `UserId` in the token; display data (name, avatar) is resolved client-side. It is the one context missing from every arrow above, because there isn't one.

## Notes on the v1 boundaries

These are deliberate choices a reviewer would otherwise flag:

- **`social` was three contexts once.** `follow`, `feed` and `engagement` (now the `likes`
  sub-domain) shipped as separate contexts in early v1 as bounded-context practice, then
  merged (#128): together they were smaller than most single contexts here — `feed` has no
  store, the other two are one table each — and the split bought nothing the `internal/`
  sub-domain packages and `InternalSlicingTest` don't already give. A later re-extraction
  stays cheap: each sub-domain keeps its own package, and `follow` / `likes` keep their own
  schema.
- **`chat` sits outside the deployable entirely.** Every other context here is designed
  to be split out later (ADR-0001) but ships inside the monolith today; `chat` inverts
  that — it never entered the monolith, shipping from day one as a separate Spring
  WebFlux service with its own Gradle build, deliberately practising a real service
  boundary and reactive programming end to end. See ADR-0014.
- **`app` depends on every context by design.** It is the composition root: it wires the
  modules together and hosts the cross-cutting infrastructure (HTTP security, the `Clock`
  bean, Flyway, OpenAPI). It is the one place the modularity is necessarily porous, and
  `ModulithStructureTest` treats it accordingly.
- **The cross-module query interfaces are pinned by consumer-contract tests (#157).**
  `LikeCounts` (both the viewer and the no-viewer form), `CommentCounts`, and
  `PublishedPosts` each have a `...ContractTest` in a `contract` test package that drives
  the published type through the interface, so a breaking shape change fails at the module
  boundary. `PublishedPostsContractTest` and `LikeCountsContractTest` are the former
  `AuthoredPostsTest` / `LikeTallyTest` promoted into `contract` (those query services have
  no surface beyond the published interface); `CommentCountsContractTest` was split out of
  `CommentThreadTest`, which keeps the write / page / delete / cleanup cases. What is
  pinned: the batch-read invariants — one row per requested id, an unknown id reads as the
  zero value, and no viewer state leaks into the no-viewer `LikeCounts` form — plus, for
  `PublishedPosts`, that `byAuthors` returns only the requested authors' posts and pages
  the whole set once and `authorOf` is empty for an unknown id. The record shapes
  themselves are not otherwise version-pinned. `FollowGraph` was a fourth such interface
  until #128 made it in-module; the structurally identical `GET /api/follows?ids=` batch
  read is deliberately left unpinned — it has no cross-module consumer, only the SPA (see
  `backend/social/CONTEXT.md`).

## Recording decisions

Architecture-level decisions live in [`docs/adr/`](./docs/adr/). Start there before changing
module boundaries, the integration style, the auth model, the database-migration layout, or
the feed strategy.
