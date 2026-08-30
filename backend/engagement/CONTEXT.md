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
