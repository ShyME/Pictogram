package me.imshy.pictogram.engagement.internal;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class LikeId implements Serializable {

    private UUID postId;
    private UUID viewerId;

    protected LikeId() {}

    LikeId(UUID postId, UUID viewerId) {
        this.postId = postId;
        this.viewerId = viewerId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LikeId that
                && Objects.equals(postId, that.postId)
                && Objects.equals(viewerId, that.viewerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, viewerId);
    }
}
