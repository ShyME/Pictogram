-- Explore (#228) is a keyset page across every author, newest first, with no author
-- filter — post_author_timeline_idx (V004) leads with author_id and can't serve that
-- global order, forcing a full scan and sort. This index lets Postgres walk it directly.
create index post_published_at_idx on post.post (published_at desc, id desc);
