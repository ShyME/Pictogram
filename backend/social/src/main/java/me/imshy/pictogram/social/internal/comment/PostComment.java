package me.imshy.pictogram.social.internal.comment;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

/** One comment as the thread read and the create response expose it. */
public record PostComment(UUID commentId, PostId postId, ViewerId viewer, String body, Instant createdAt) {}
