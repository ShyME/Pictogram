# Follow

The directed graph of who follows whom. Nothing else — no feed, no counts beyond what the
graph itself answers.

## Language

**Follow**:
A directed relationship from a follower to a followed user. Creating it is "following";
removing it is "unfollowing". At most one follow exists per ordered pair.
_Avoid_: Friend, connection, subscription, link

**Follower**:
The user on the acting end of a follow — the one who follows.
_Avoid_: Fan, subscriber

**Followed user**:
The user on the receiving end of a follow — the one being followed.
_Avoid_: Followee, target, idol

**Viewer**:
The user making the current request, on whose behalf a follow or unfollow is performed.
Carried as a `ViewerId` so it cannot be confused with the followed user.
_Avoid_: Current user, actor, me, principal

**Self-follow**:
A follow where follower and followed user are the same. Rejected.
_Avoid_: Loop, reflexive follow

**Follower count / following count**:
How many follows point at a user, and how many point away from them. Derived live from the
graph — there is no stored total.
_Avoid_: Reach, popularity, stats

**Follower list / following list**:
The paged list of the users who follow someone, or of the users someone follows — newest
follow first, keyset-paged on the edge's `followedAt` (#57). A web-layer read only, not
part of the published `FollowGraph`: `feed` needs the flat "who does this viewer follow"
list, not a page of it.
_Avoid_: Followers page (the screen), connections

## Published interface

`FollowGraph` answers, for other contexts (queries only — ADR-0002): the users a viewer
follows (feed's fan-out-on-read input — ADR-0003), the follower and following counts, and
whether one user follows another. `follow` emits `UserFollowed` / `UserUnfollowed` on a real
state change; an idempotent no-op emits nothing.

The paged **follower list / following list** reads (#57) are deliberately *not* on
`FollowGraph` — they serve the SPA's list screens through `follow`'s own web layer
(`GET /api/follows/{userId}/followers`, `/following`), and `feed` has no use for them.

The **batch relationship read** (#59, `GET /api/follows?ids=`) is the same story: one call
returns the viewer's standing — counts and follow flag — with each of a set of users, so a
list screen renders its follow buttons from a warmed cache instead of one
`GET /api/follows/{id}` per row. Web-layer only (`FollowRelationships` in `follow.internal`);
`feed` fans out over the flat `usersFollowedBy` list, not a batch.
