-- The follower / following list screens (#57) page newest-relationship-first on a keyset
-- cursor (shared.http.Cursor is (Instant, UUID)), so an edge needs the instant it was
-- created. #17 deliberately left it off — counts and "is A following B" don't need it —
-- but a paged list does. Backfill existing edges to now(); there is no real creation time
-- to recover and the exact value only affects tie ordering among today's rows.
alter table follow.follow
    add column followed_at timestamptz not null default now();

alter table follow.follow
    alter column followed_at drop default;

-- followersOf(user) pages `where followed_id = ? order by followed_at desc, id desc`;
-- followingOf(user) does the same on follower_id. Two covering indexes, one per direction.
create index follow_followers_page_idx on follow.follow (followed_id, followed_at desc, id desc);
create index follow_following_page_idx on follow.follow (follower_id, followed_at desc, id desc);

-- follow_followed_idx (followed_id) from V005 is now a prefix of follow_followers_page_idx,
-- which serves follower_count(user) just as well — drop the redundant one.
drop index follow.follow_followed_idx;
