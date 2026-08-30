# Feed

A viewer's home view: the posts of the people they follow, newest first. Feed owns no posts
and no follow data of its own — it assembles the view from the other contexts.

## Language

**Feed**:
The ordered list of posts from the people a viewer follows, most recent first, specific to
that viewer.
_Avoid_: Wall, timeline, home, stream, dashboard

**Viewer**:
The user whose feed is being produced. Carried as a `ViewerId`.
_Avoid_: Current user, subscriber, me

**Page**:
A contiguous slice of a feed returned by one request, with a cursor for the next slice.
_Avoid_: Batch, chunk, window

**Cursor**:
An opaque token marking a position in a feed, so the next page continues exactly where the
last one ended regardless of posts added in between (keyset, not offset).
_Avoid_: Offset, page number, token (bare)
