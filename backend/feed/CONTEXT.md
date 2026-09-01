# Feed

A viewer's home view: the posts of the people they follow, newest first. Feed owns no posts
and no follow data of its own — it assembles the view from the other contexts.

## How it is assembled

`FeedQuery` is the port (ADR-0003). Its v1 implementation, `FanOutOnReadFeed`, is
fan-out-on-read: for each request it asks `follow` (`FollowGraph.usersFollowedBy`) who the
viewer follows, then asks `post` (`PublishedPosts.byAuthors`) for the next keyset page of
those authors' posts, newest first. Nothing is stored, so a deleted post or a removed
follow simply stops showing up on the next read. Per-card enrichment — author name, image,
likes — is the client's job (ADR-0005), not the feed's.

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

**Card**:
One post as it is rendered in the feed — image, caption, author, timestamp. `GET /api/feed`
returns the bare `FeedPost` (post id, author id, media id, caption, published-at); the
client batches in the author and image to build the card.
_Avoid_: Item, entry, row, tile
