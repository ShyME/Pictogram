package me.imshy.pictogram.social;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

/**
 * A viewer commented on a post. Fires once per new comment; a comment can't be edited, so
 * there is no "changed" counterpart. {@code commentedAt} is the {@code Clock} instant the
 * comment was recorded — the same value stored on the comment, captured the same way as
 * {@link PostLiked#likedAt()}. No context consumes this in v1; it is the module's forward
 * contract (comment counts, notifications).
 */
public record PostCommented(PostId postId, UUID commentId, ViewerId viewer, Instant commentedAt) {}
