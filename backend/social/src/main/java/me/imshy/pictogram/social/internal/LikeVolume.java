package me.imshy.pictogram.social.internal;

// Sits above the likes/comment/follow slices, same reason as FollowGraph: the one visible
// seam a cross-sub-domain reader (SocialStatsService) is allowed to depend on.
//
// Not added to LikeCounts: that published interface is @MockitoBean'd by name in other
// tests (its per-post batch-read shape), and folding an unrelated total() onto the same
// bean would make such a mock silently stop satisfying this interface too (#233 hit
// exactly this combining PostStats onto post's AuthoredPosts/PublishedPosts).
//
// A separate near-identical CommentVolume, not one shared `total()` interface: the two are
// implemented by different beans (LikeTally, CommentThread) and SocialStatsService injects
// both by type — one shared interface would need @Qualifier-style disambiguation for no
// real gain over two one-line interfaces.
public interface LikeVolume {

    long total();
}
