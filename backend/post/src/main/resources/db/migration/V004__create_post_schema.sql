-- post owns the `post` schema (ADR-0009). A post is keyed by its own
-- PostId; `author_id` and `media_id` are UserId / MediaId values with no foreign key to
-- identity or media — contexts reference each other by ID value only (ADR-0002).
create schema if not exists post;

-- One row per published post. A post is immutable after publish (post/CONTEXT.md); the only
-- later change is deletion, which removes the row. `caption` is optional and null when the
-- author left it blank; it is at most 2200 characters (enforced in the domain).
create table post.post
(
    id           uuid        primary key,
    author_id    uuid        not null,
    media_id     uuid        not null,
    caption      text,
    published_at timestamptz not null
);

-- The profile grid and the feed both read a page of an author's posts newest-first with the
-- id as the keyset tiebreaker (shared.http.Cursor), so the index covers exactly that order.
create index post_author_timeline_idx on post.post (author_id, published_at desc, id desc);
