-- follow owns the `follow` schema (ADR-0009). An edge references two users by their
-- UserId value; there is no foreign key to identity.app_user or to profile — contexts
-- reference each other by ID value only (ADR-0002).
create schema if not exists follow;

-- One row per directed follow edge. `id` is a surrogate single-column key for JPA; the
-- real identity of an edge is the (follower_id, followed_id) pair, which is unique — that
-- constraint is what makes "follow" idempotent and blocks a duplicate edge under a race.
create table follow.follow
(
    id          uuid primary key,
    follower_id uuid not null,
    followed_id uuid not null,
    unique (follower_id, followed_id)
);

-- follower_count(user) is `where followed_id = ?`; the unique constraint above already
-- covers the (follower_id, ...) direction used by following_count and "is A following B".
create index follow_followed_idx on follow.follow (followed_id);
