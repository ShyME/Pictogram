package me.imshy.pictogram.post;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * An author published a post. Carries the ids a consumer needs to react without calling
 * back — the fan-out-on-write feed would append {@code postId} to each follower's timeline,
 * a notifier would announce it. In v1 nothing consumes this; it is post's forward contract
 * (ADR-0002, CONTEXT-MAP).
 */
public record PostPublished(PostId postId, UserId authorId, MediaId mediaId, Instant publishedAt) {
}
