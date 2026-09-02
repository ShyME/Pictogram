-- The real identity of a like is the (post_id, viewer_id) pair. V007 gave the row a separate
-- single-column `id` only to hand JPA a simple key; nothing ever referenced it. Promote the
-- pair to the primary key and drop the surrogate. Dropping `id` takes the old primary-key
-- constraint with it; the former `unique (post_id, viewer_id)` is now the primary key, so it
-- goes too.
alter table engagement.post_like
    drop column id;

alter table engagement.post_like
    drop constraint post_like_post_id_viewer_id_key;

alter table engagement.post_like
    add primary key (post_id, viewer_id);

-- post_like_post_idx (post_id) is now a prefix of the (post_id, viewer_id) primary key, which
-- serves like_count(post) just as well — drop the redundant one.
drop index engagement.post_like_post_idx;
