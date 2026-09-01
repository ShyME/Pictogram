package me.imshy.pictogram.follow.internal;

import java.util.List;
import me.imshy.pictogram.follow.FollowGraph;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.stereotype.Service;

@Service
public class FollowDirectory implements FollowGraph {

    private final Follows follows;

    FollowDirectory(Follows follows) {
        this.follows = follows;
    }

    @Override
    public List<UserId> usersFollowedBy(ViewerId viewer) {
        return follows.followedIdsOf(viewer.value()).stream().map(UserId::new).toList();
    }

    @Override
    public long followerCount(UserId user) {
        return follows.countByFollowedId(user.value());
    }

    @Override
    public long followingCount(UserId user) {
        return follows.countByFollowerId(user.value());
    }

    @Override
    public boolean isFollowing(ViewerId viewer, UserId user) {
        return follows.existsByFollowerIdAndFollowedId(viewer.value(), user.value());
    }
}
