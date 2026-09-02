# Engagement

Reactions people attach to a post. Likes in v1; comments are designed for and added next.
The post itself is unaware of engagement — counts and "did I react" are answered here.

## Language

**Engagement**:
The umbrella for reactions attached to a post — likes now, comments later. Not a word end
users see.
_Avoid_: Interaction, activity, social, reaction (as the umbrella)

**Like**:
An at-most-one-per-user endorsement of a post. Liking again is a no-op; unliking when not
liked is a no-op.
_Avoid_: Favourite, heart, upvote, star, reaction

**Comment** (designed for, not built in v1):
A short piece of free text a user attaches to a post. Flat — no replies, no threading.
_Avoid_: Reply, note, annotation, thread

**Viewer**:
The user performing the like or unlike. Carried as a `ViewerId` so it is never confused
with the post's author.
_Avoid_: Current user, actor, me

## Published interface

`LikeCounts.of(ViewerId, Collection<PostId>)` — the like count and the viewer's own like
state for every requested post, in one call (ADR-0005, no N+1). A post nobody has liked
reads as `(id, 0, false)`; the batch never omits a requested id. `LikeCounts.of(Collection<PostId>)`
is the same read with no viewer — every row reports `likedByViewer` as `false`.

Exposed over HTTP as `GET /api/engagement/likes?postIds=` plus
`PUT`/`DELETE /api/engagement/likes/{postId}`. The batch read tolerates an anonymous
caller (a signed-out visitor on a public profile sees the true count, `likedByViewer`
`false`); liking and unliking require a viewer.

## Events

`PostLiked` / `PostUnliked` fire only on a real state change — liking an already-liked
post or unliking one that was never liked is a silent no-op. No context consumes them in
v1; they are the module's forward contract (comment counts, notifications).

`PostLiked.likedAt` and `PostUnliked.unlikedAt` are both the engagement `Clock` instant at
which the change was recorded, captured the same way, so for one `(viewer, post)` a later
`unlikedAt` is never earlier than the matching `likedAt` — the two are directly comparable.

## Rules

At most one like per `(viewer, post)` — that pair **is** the row's primary key (there is no
surrogate id), and that constraint is what makes a concurrent double-like idempotent. There
is **no self-like rule**: liking your own post is allowed, because engagement has no notion
of a post's author (contrast the self-follow guard in `follow`).

`Liking.like()` checks for an existing like before it saves, rather than relying only on the
`DataIntegrityViolationException` from the primary key. The check is the fast path *and* what
lets a caller run `like()` inside their own transaction: a constraint violation would doom
that transaction whether or not the exception is caught. `LikingTest` pins this.
