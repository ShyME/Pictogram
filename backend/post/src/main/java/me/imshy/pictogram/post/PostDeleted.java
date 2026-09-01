package me.imshy.pictogram.post;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * An author permanently deleted a post. Hard delete — the row is gone and there is no undo
 * (post/CONTEXT.md). Carries {@code mediaId} so a consumer can react without calling back:
 * the orphan-media collector (#16) uses it to learn the image may now be unreferenced, a
 * fan-out-on-write feed would drop {@code postId} from each follower's timeline. Nothing
 * consumes it in v1; it is post's forward contract alongside {@link PostPublished}
 * (ADR-0002, CONTEXT-MAP).
 */
public record PostDeleted(PostId postId, UserId authorId, MediaId mediaId, Instant deletedAt) {
}
