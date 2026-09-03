-- Promote the (follower_id, followed_id) pair to the primary key and drop the surrogate
-- `id`, matching engagement.post_like (V008). The #57 keyset pages no longer need it — they
-- tie-break on the pair column that varies within a page (see the index rebuild below).
--
-- Dropping `id` cascades: it takes the old primary-key constraint and both #57 covering
-- indexes (V006 built them ending in `id desc`) with it. Only the former
-- `unique (follower_id, followed_id)` is left to drop by hand before it becomes the PK.
alter table follow.follow
    drop column id;

alter table follow.follow
    drop constraint follow_follower_id_followed_id_key;

alter table follow.follow
    add primary key (follower_id, followed_id);

-- Rebuild the #57 covering indexes, each tie-breaking on the pair column that varies within
-- its page so the keyset ordering stays total.
create index follow_followers_page_idx on follow.follow (followed_id, followed_at desc, follower_id desc);
create index follow_following_page_idx on follow.follow (follower_id, followed_at desc, followed_id desc);
