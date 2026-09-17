package me.imshy.pictogram.social.internal;

// Sits above the likes/comment/follow slices, same reason as FollowGraph: the one visible
// seam a cross-sub-domain reader (SocialStatsService) is allowed to depend on. See
// LikeVolume for why this isn't folded onto CommentCounts, or merged with LikeVolume into
// one shared interface.
public interface CommentVolume {

    long total();
}
