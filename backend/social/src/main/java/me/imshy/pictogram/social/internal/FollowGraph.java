package me.imshy.pictogram.social.internal;

import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;

public interface FollowGraph {

    List<UserId> usersFollowedBy(ViewerId viewer);

    long followerCount(UserId user);

    long followingCount(UserId user);

    boolean isFollowing(ViewerId viewer, UserId user);

    // Added here rather than as a new one-method interface (contrast LikeVolume /
    // CommentVolume): FollowGraph is already this exact kind of internal
    // cross-sub-domain
    // seam, and — unlike LikeCounts/CommentCounts — nothing mocks it by name, so
    // there's no
    // risk of an unrelated mock silently stopping satisfying it.
    long totalFollows();
}
