# Feed is fan-out-on-read, behind a port

The feed can be built two ways: compute it on demand from the follow graph and posts
(fan-out-on-read), or maintain a materialised per-user feed updated by events
(fan-out-on-write). We are doing **fan-out-on-read** in v1, exposed through a `FeedQuery`
port inside the `feed` module.

At portfolio scale the on-demand query is cheap and always consistent, and it keeps v1
small. The port means switching to fan-out-on-write later is an implementation change with
**no API change** — `GET /api/feed` and its cursor contract stay identical. Fan-out-on-write
is where the interesting event choreography lives and is the documented intended evolution.

## Consequences

- `feed` calls `follow` and `post` published interfaces synchronously to assemble a page.
- The feed has no store of its own in v1; there is nothing to clean up when a post is
  deleted or a follow is removed.
