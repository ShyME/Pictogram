# Pictogram — Context Map

Pictogram is a small Instagram-like web app: people upload square image posts, follow
other people, and scroll a feed of posts from the people they follow. It is a portfolio
project built to practise **Domain-Driven Design** and **Test-Driven Development**, as a
modular monolith whose module boundaries are drawn so they could later be extracted into
separate services.

Each module below is one bounded context, one Gradle subproject, and one Spring Modulith
module. `social` carries four internal sub-domains (`follow`, `feed`, `likes`, `comment`) —
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

## Relationships

- **All contexts → `shared-kernel`**: depend on it for ID value types. Contexts reference each other's data **by ID only** — never object references, never foreign keys across schemas.
- **identity → profile**: `identity` emits `UserRegistered` when a person first authenticates. In v1 the `Profile` is not created from that event — it is created by the **onboarding** step when the person picks a username. A user without a profile is a legitimate "not yet onboarded" state.
- **social (feed) → post**: `social`'s feed sub-domain calls `post`'s published interface (`PublishedPosts`) synchronously to assemble a page (fan-out-on-read), behind its own `FeedQuery` port (ADR-0003). The other half of the fan-out — the follow graph — is now an in-module call (`FollowGraph` in `social.internal`). The feed has no store.
- **post → media**: a `Post` holds a `MediaId`, and checks ownership via `MediaCatalog` on publish. `media`'s orphan collection needs to know which media a post still references; since the Gradle arrow only runs this way, `media` declares that as a port (`PostReferences`) and `post` provides the adapter.
- **social → post**: a `Like` and a `Comment` each hold a `PostId`. `social`'s `comment` sub-domain consumes `post`'s `PostDeleted` to hard-delete that post's thread (the one real cross-context event reaction in v1 — synchronous, no registry, ADR-0002), and calls `PublishedPosts.authorOf` to check comment-delete permission. `PostPublished` still has no consumer.
- **Events emitted, mostly unconsumed in v1**: `UserRegistered`, `UserFollowed`, `UserUnfollowed`, `PostPublished`, `PostDeleted`, `PostLiked`, `PostUnliked`, `PostCommented`, `CommentDeleted`, `ProfileUpdated`. They exist as each module's public contract so consumers (fan-out-on-write feed, notifications) can be added later without touching producers. `PostDeleted` is the one with a consumer (comment-thread cleanup).

## Notes on the v1 boundaries

These are deliberate choices a reviewer would otherwise flag:

- **`social` was three contexts once.** `follow`, `feed` and `engagement` (now the `likes`
  sub-domain) shipped as separate contexts in early v1 as bounded-context practice, then
  merged (#128): together they were smaller than most single contexts here — `feed` has no
  store, the other two are one table each — and the split bought nothing the `internal/`
  sub-domain packages and `InternalSlicingTest` don't already give. A later re-extraction
  stays cheap: each sub-domain keeps its own package, and `follow` / `likes` keep their own
  schema.
- **`app` depends on every context by design.** It is the composition root: it wires the
  modules together and hosts the cross-cutting infrastructure (HTTP security, the `Clock`
  bean, Flyway, OpenAPI). It is the one place the modularity is necessarily porous, and
  `ModulithStructureTest` treats it accordingly.
- **The published interfaces are not signature-pinned in v1.** `LikeCounts` and
  `CommentCounts` have one caller each (their sub-domain's web layer); `PublishedPosts` now
  has two (`feed`'s fan-out and `comment`'s delete-permission check). Each shape is still
  exercised through its implementation's own tests (`AuthoredPostsTest`, `LikeTallyTest`,
  `CommentTallyTest`) rather than a dedicated consumer-contract test; before an extraction,
  promote those to a contract test per interface so a breaking change fails at the boundary.
  (`FollowGraph` was a fourth such interface until #128 made it in-module.)

## Recording decisions

Architecture-level decisions live in [`docs/adr/`](./docs/adr/). Start there before changing
module boundaries, the integration style, the auth model, the database-migration layout, or
the feed strategy.
