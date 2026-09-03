package me.imshy.pictogram.social;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

/**
 * A viewer liked a post. {@code likedAt} is the {@code Clock} instant at which the like
 * was recorded — the same value stored on the like and captured the same way as {@link
 * PostUnliked#unlikedAt()}, so the two are directly comparable.
 */
public record PostLiked(PostId postId, ViewerId viewer, Instant likedAt) {}
