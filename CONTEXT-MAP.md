# Pictogram — Context Map

Pictogram is a small Instagram-like web app: people upload square image posts, follow
other people, and scroll a feed of posts from the people they follow. It is a portfolio
project built to practise **Domain-Driven Design** and **Test-Driven Development**, as a
modular monolith whose module boundaries are drawn so they could later be extracted into
separate services.

Each module below is one bounded context, one Gradle subproject, and one Spring Modulith
module. `shared-kernel` is the single exception: a whitelisted module every context may
depend on. It holds the ID value types (`UserId`, `PostId`, `MediaId`, `ViewerId`) and,
in its `http` sub-package, the cross-cutting HTTP edge conventions — Problem Details, the
pagination envelope, current-user resolution (ADR-0008). No domain behaviour, entities, or
persistence.

## Contexts

- [identity](./backend/identity/CONTEXT.md): authenticates people via Google and issues Pictogram's own tokens.
- [profile](./backend/profile/CONTEXT.md): the public face of a user — username, display name, bio — and the onboarding step that creates it.
- [media](./backend/media/CONTEXT.md): stores uploaded images as bytes, re-encoded to one canonical square format.
- [post](./backend/post/CONTEXT.md): the `Post` — one image plus an optional caption, published by an author.
- [follow](./backend/follow/CONTEXT.md): the directed follow graph.
- [feed](./backend/feed/CONTEXT.md): a viewer's home view, assembled from the follow graph and posts.
- [engagement](./backend/engagement/CONTEXT.md): reactions attached to a post — likes now, comments later.

## Relationships

- **All contexts → `shared-kernel`**: depend on it for ID value types. Contexts reference each other's data **by ID only** — never object references, never foreign keys across schemas.
- **identity → profile**: `identity` emits `UserRegistered` when a person first authenticates. In v1 the `Profile` is not created from that event — it is created by the **onboarding** step when the person picks a username. A user without a profile is a legitimate "not yet onboarded" state.
- **feed → follow, feed → post**: `feed` calls the published interfaces of `follow` and `post` synchronously to assemble a page (fan-out-on-read).
- **post → media**: a `Post` holds a `MediaId`. `media`'s orphan-collection reads which media are still referenced by a post.
- **engagement → post**: a `Like` holds a `PostId`. `post` emits `PostPublished` / `PostDeleted`; no context consumes them in v1 (they are the module's forward contract).
- **Events emitted, mostly unconsumed in v1**: `UserRegistered`, `UserFollowed`, `UserUnfollowed`, `PostPublished`, `PostDeleted`, `PostLiked`, `PostUnliked`, `ProfileUpdated`. They exist as each module's public contract so consumers (fan-out-on-write feed, notifications, comment counts) can be added later without touching producers.

## Recording decisions

Architecture-level decisions live in [`docs/adr/`](./docs/adr/) (`0001`–`0009`). Start
there before changing module boundaries, the integration style, the auth model, the
database-migration layout, or the feed strategy.
