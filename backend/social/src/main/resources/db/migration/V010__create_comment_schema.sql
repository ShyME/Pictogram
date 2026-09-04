-- The `comment` sub-domain owns the `comment` schema (ADR-0009). A comment references a
-- post and its author by their PostId / ViewerId value; there is no foreign key to post or
-- identity — contexts reference each other by ID value only (ADR-0002).
create schema if not exists comment;

-- One row per comment. Unlike `follow` / `likes` — whose identity is a natural pair — a
-- comment has no natural key: a viewer may comment on the same post any number of times.
-- `id` is therefore the real primary key, assigned by the application.
create table comment.comment
(
    id         uuid        primary key,
    post_id    uuid        not null,
    viewer_id  uuid        not null,
    body       text        not null,
    created_at timestamptz not null
);

-- The thread read is `where post_id = ? order by created_at, id` (keyset, oldest first).
create index comment_thread_idx on comment.comment (post_id, created_at, id);
