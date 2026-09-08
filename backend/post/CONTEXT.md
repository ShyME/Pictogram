# Post

The unit of content in Pictogram: one image plus an optional caption, published by one
person. A post does not change after it is published — it is only created or deleted.

## Published interfaces

- `PublishedPosts.byAuthors(authors, cursor, limit)` — a keyset page of the posts by a set
  of authors, newest first. `feed` calls this to assemble a fan-out-on-read page (ADR-0003).
- `PublishedPosts.authorOf(post)` — the author of a post, empty for an unknown id.
  `social`'s `comment` sub-domain calls it to gate comment deletion.
- Events `PostPublished` / `PostDeleted` — the module's forward contract, unconsumed in v1.

`PublishedPostsContractTest` (`post.contract`, #157) drives the published type as `feed`
and `comment` do — distinct from `AuthoredPostsTest`, which tests the implementation — and
pins the consumer invariants: only the requested authors' posts come back, an author with
no posts contributes nothing rather than failing, the keyset page walks every post once,
and `authorOf` is empty for an unknown id.

## Language

**Post**:
One image (referenced by `MediaId`) plus an optional caption, published by an author at a
point in time. Immutable once published; the only later change is deletion.
_Avoid_: Picture, photo, upload, entry, gram

**Author**:
The user who published the post. Referenced by `UserId`.
_Avoid_: Owner, poster, creator, user

**Caption**:
Optional free text on a post, up to 2200 characters.
_Avoid_: Description, text, body, message

**Deletion**:
Permanent removal of a post. Hard, not soft — the record is gone and `PostDeleted` is
emitted. There is no trash or undo.
_Avoid_: Archive, hide, soft delete, remove
