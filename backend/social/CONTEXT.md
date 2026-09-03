# Social

Everything that connects one user to another's posts: the **follow graph**, the **feed**
assembled from it, and the **likes** on a post.

These were three separate contexts in early v1 (`follow`, `feed`, `engagement` — the last
now the `likes` sub-domain). They were merged into one (#128) because together they are
smaller than most single contexts here —
`feed` has no store, `follow` and `likes` are one table each — and the split bought
nothing the module boundary inside `social` doesn't already give. A later re-extraction is
still cheap: each sub-domain keeps its own package, and `follow` / `likes` keep their
own schema.

Comments are designed for and come next; they will be their own `social.internal.comment`
sub-domain and schema, not folded into `likes`.

## Internal shape

`social.internal` has one package per sub-domain — `follow`, `feed`, `likes` — and
`InternalSlicingTest` forbids them from reaching into each other. The one seam between them
is **`FollowGraph`** (in `social.internal`, one level up from the slices): `feed` reads it
to fan out, `follow` implements it. It sits above the slices deliberately, so the
feed → follow direction is a single visible interface rather than a slice violation.

## Follow

The directed graph of who follows whom. Nothing else — no feed, no counts beyond what the
graph itself answers.

**Follow**: a directed relationship from a follower to a followed user. Creating it is
"following"; removing it is "unfollowing". At most one follow exists per ordered pair.
_Avoid_: Friend, connection, subscription, link

**Follower**: the user on the acting end of a follow — the one who follows.
_Avoid_: Fan, subscriber

**Followed user**: the user on the receiving end of a follow — the one being followed.
_Avoid_: Followee, target, idol

**Viewer**: the user making the current request, on whose behalf a follow or unfollow is
performed. Carried as a `ViewerId` so it cannot be confused with the followed user.
_Avoid_: Current user, actor, me, principal

**Self-follow**: a follow where follower and followed user are the same. Rejected.
_Avoid_: Loop, reflexive follow

**Follower count / following count**: how many follows point at a user, and how many point
away from them. Derived live from the graph — there is no stored total.
_Avoid_: Reach, popularity, stats

**Follower list / following list**: the paged list of the users who follow someone, or of
the users someone follows — newest follow first, keyset-paged on the edge's `followedAt`
(#57). A web-layer read only, not part of `FollowGraph`.
_Avoid_: Followers page (the screen), connections

### The edge row

The `(follower_id, followed_id)` pair **is** the row's primary key (V009 — there is no
surrogate `id`, matching `likes.post_like`). The #57 follower/following keyset pages
tie-break the `followed_at` sort on the pair column that varies within a page: a followers
page fixes `followed_id`, so it tie-breaks on `follower_id`; a following page fixes
`follower_id`, so it tie-breaks on `followed_id`. In both directions the varying column is
the id of the user the page lists, which is exactly what `shared.http.Cursor`'s UUID
component already carried — so the cursor shape is unchanged. `FollowListTest` pins the
tie-break column per direction.

## Feed

A viewer's home view: the posts of the people they follow, newest first. Feed owns no posts
and no follow data of its own — it assembles the view from `follow` and `post`.

**Feed**: the ordered list of posts from the people a viewer follows, most recent first,
specific to that viewer.
_Avoid_: Wall, timeline, home, stream, dashboard

**Page**: a contiguous slice of a feed returned by one request, with a cursor for the next
slice.
_Avoid_: Batch, chunk, window

**Cursor**: an opaque token marking a position in a feed, so the next page continues
exactly where the last one ended regardless of posts added in between (keyset, not offset).
_Avoid_: Offset, page number, token (bare)

**Card**: one post as it is rendered in the feed — image, caption, author, timestamp.
`GET /api/feed` returns the bare `FeedPost` (post id, author id, media id, caption,
published-at); the client batches in the author and image to build the card (ADR-0005).
_Avoid_: Item, entry, row, tile

### How it is assembled

`FeedQuery` is the port (ADR-0003). Its v1 implementation, `FanOutOnReadFeed`, is
fan-out-on-read: for each request it asks `FollowGraph.usersFollowedBy` who the viewer
follows, then asks `post` (`PublishedPosts.byAuthors`) for the next keyset page of those
authors' posts, newest first. Nothing is stored, so a deleted post or a removed follow
simply stops showing up on the next read.

## Likes

An endorsement a viewer attaches to a post. The post itself is unaware of likes — the
count and "did I like it" are answered here.

**Like**: an at-most-one-per-user endorsement of a post. Liking again is a no-op; unliking
when not liked is a no-op.
_Avoid_: Favourite, heart, upvote, star, reaction

**Viewer**: the user performing the like or unlike. Carried as a `ViewerId` so it is never
confused with the post's author.
_Avoid_: Current user, actor, me

**Comment** (designed for, not built): a short piece of free text a user attaches to a
post. Flat — no replies, no threading. Its own sub-domain when built, not part of `likes`.
_Avoid_: Reply, note, annotation, thread

### Rules

At most one like per `(viewer, post)` — that pair **is** the row's primary key (there is no
surrogate id), and that constraint is what makes a concurrent double-like idempotent. There
is **no self-like rule**: liking your own post is allowed, because the `likes` sub-domain
has no notion of a post's author (contrast the self-follow guard in `follow`).

`Liking.like()` checks for an existing like before it saves, rather than relying only on
the `DataIntegrityViolationException` from the primary key. The check is the fast path *and*
what lets a caller run `like()` inside their own transaction: a constraint violation would
doom that transaction whether or not the exception is caught. `LikingTest` pins this.

## Published interface

Queries only — ADR-0002. `FollowGraph` is **not** on this list: it is module-internal now
that its only caller, `feed`, lives in the same module.

- **`LikeCounts.of(ViewerId, Collection<PostId>)`** — the like count and the viewer's own
  like state for every requested post, in one call (ADR-0005, no N+1). A post nobody has
  liked reads as `(id, 0, false)`; the batch never omits a requested id.
  `LikeCounts.of(Collection<PostId>)` is the same read with no viewer — every row reports
  `likedByViewer` as `false`.

The paged **follower list / following list** reads (#57) and the **batch relationship
read** (#59, `GET /api/follows?ids=`) serve the SPA's list screens through `follow`'s own
web layer only — they are deliberately not on any published interface.

Over HTTP:

- `GET /api/feed` — the viewer's feed page.
- `PUT`/`DELETE /api/follows/{userId}`, `GET /api/follows/{userId}`,
  `GET /api/follows?ids=`, `GET /api/follows/{userId}/followers`, `/following`.
- `GET /api/likes?postIds=`, `PUT`/`DELETE /api/likes/{postId}`.
  The batch read tolerates an anonymous caller; liking and unliking require a viewer.

## Events

`UserFollowed` / `UserUnfollowed` and `PostLiked` / `PostUnliked` fire only on a real state
change — an idempotent no-op emits nothing. No context consumes them in v1; they are the
module's forward contract (fan-out-on-write feed, comment counts, notifications).

`PostLiked.likedAt` and `PostUnliked.unlikedAt` are both the `Clock` instant at which the
change was recorded, captured the same way, so for one `(viewer, post)` a later
`unlikedAt` is never earlier than the matching `likedAt` — the two are directly comparable.

## Schema

`follow` and `likes` each own a Postgres schema of that name (`feed` has no store; the
`likes` schema is plural because `like` is a SQL reserved word). The `social` module owning
two schemas is the one place ADR-0009's "one schema per context" doesn't hold literally — a
consequence of the #128 merge, and harmless: each schema is still self-contained and would
move with its sub-domain on a re-extraction.
