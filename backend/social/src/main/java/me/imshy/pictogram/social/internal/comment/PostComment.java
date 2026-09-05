package me.imshy.pictogram.social.internal.comment;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

public record PostComment(UUID commentId, PostId postId, ViewerId viewer, String body, Instant createdAt) {
}
