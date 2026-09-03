package me.imshy.pictogram.social.internal;

import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;

public interface FollowGraph {

    List<UserId> usersFollowedBy(ViewerId viewer);

    long followerCount(UserId user);

    long followingCount(UserId user);

    boolean isFollowing(ViewerId viewer, UserId user);
}
