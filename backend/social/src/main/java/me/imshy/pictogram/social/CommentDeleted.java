package me.imshy.pictogram.social;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

public record CommentDeleted(PostId postId, UUID commentId, ViewerId viewer, Instant deletedAt) {
}
