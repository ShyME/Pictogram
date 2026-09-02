-- engagement owns the `engagement` schema (ADR-0009). A like references a post and a
-- viewer by their PostId / ViewerId value; there is no foreign key to post or identity —
-- contexts reference each other by ID value only (ADR-0002).
create schema if not exists engagement;

-- One row per (viewer, post) like. `id` is a surrogate single-column key for JPA; the real
-- identity of a like is the (post_id, viewer_id) pair, which is unique — that constraint is
-- what makes "like" at-most-one-per-viewer and idempotent under a race. `like` is a
-- reserved word, hence `post_like`.
create table engagement.post_like
(
    id        uuid        primary key,
    post_id   uuid        not null,
    viewer_id uuid        not null,
    liked_at  timestamptz not null,
    unique (post_id, viewer_id)
);

-- like_count(post) is `where post_id = ?`; the unique constraint above already covers the
-- (post_id, viewer_id) direction used by "has this viewer liked this post".
create index post_like_post_idx on engagement.post_like (post_id);
