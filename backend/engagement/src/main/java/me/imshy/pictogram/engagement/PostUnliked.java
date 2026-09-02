package me.imshy.pictogram.engagement;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

/**
 * A viewer removed their like from a post. {@code unlikedAt} is the engagement {@code Clock} instant
 * at which the unlike was recorded, captured the same way as {@link PostLiked#likedAt()}; for a given
 * viewer and post, {@code unlikedAt} is never earlier than the matching {@code PostLiked.likedAt}.
 */
public record PostUnliked(PostId postId, ViewerId viewer, Instant unlikedAt) {}
