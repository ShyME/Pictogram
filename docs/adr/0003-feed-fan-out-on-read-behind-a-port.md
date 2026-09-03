# Feed is fan-out-on-read, behind a port

- **Status:** Accepted
- **Relates to:** ADR-0002 (the synchronous published-interface query path)

The feed can be built two ways: compute it on demand from the follow graph and posts
(fan-out-on-read), or maintain a materialised per-user feed updated by events
(fan-out-on-write). We are doing **fan-out-on-read** in v1, exposed through a `FeedQuery`
port inside `social` (the `feed` sub-domain — `follow`, `feed` and `engagement` merged
into one module in #128, the last now the `likes` sub-domain).

At portfolio scale the on-demand query is cheap and always consistent, and it keeps v1
small. The port means switching to fan-out-on-write later is an implementation change with
**no API change** — `GET /api/feed` and its cursor contract stay identical. Fan-out-on-write
is where the interesting event choreography lives and is the documented intended evolution.

## Consequences

- The feed reads the follow graph and `post`'s published interface synchronously to
  assemble a page. The port is `FeedQuery` (in `social.internal.feed`), with
  `FanOutOnReadFeed` the v1 implementation: it reads `FollowGraph.usersFollowedBy`
  (`social.internal`, an in-module call since #128), then `post.PublishedPosts.byAuthors`
  — a keyset query interface `post` exposes for this.
- The cursor contract is `shared.http.Cursor` (keyset on `publishedAt, id`), identical to
  the profile grid's, so `GET /api/feed` stays stable across a later switch to
  fan-out-on-write.
- The feed has no store of its own in v1; there is nothing to clean up when a post is
  deleted or a follow is removed.
