package me.imshy.pictogram.social;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

/**
 * A comment was deleted. {@code viewer} is who removed it — the comment's own author or the
 * post's author (the only two allowed), captured the same way as {@link PostUnliked#viewer()}.
 * Fires once per real deletion; deleting a comment that isn't there is a silent no-op. The
 * cascade that clears a thread when its post is deleted does <em>not</em> fire this — that
 * post already announced its own {@code PostDeleted}. No context consumes this in v1; it is
 * the module's forward contract (notifications).
 */
public record CommentDeleted(PostId postId, UUID commentId, ViewerId viewer, Instant deletedAt) {}
