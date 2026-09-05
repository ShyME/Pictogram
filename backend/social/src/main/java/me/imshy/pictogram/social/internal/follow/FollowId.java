package me.imshy.pictogram.social.internal.follow;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class FollowId implements Serializable {

    private UUID followerId;
    private UUID followedId;

    protected FollowId() {
    }

    FollowId(UUID followerId, UUID followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FollowId that && Objects.equals(followerId, that.followerId)
            && Objects.equals(followedId, that.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedId);
    }
}
