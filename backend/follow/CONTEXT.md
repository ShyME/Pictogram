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
