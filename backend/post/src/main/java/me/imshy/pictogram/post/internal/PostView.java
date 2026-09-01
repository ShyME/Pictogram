package me.imshy.pictogram.post.internal;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * A read of a {@link Post} for the module's web layer — what the publish response returns
 * and what a grid page is a list of. {@code caption} is {@code null} when the author left it
 * blank. The client turns {@code mediaId} into an image URL ({@code /api/media/{id}/...}).
 */
public record PostView(PostId postId, UserId authorId, MediaId mediaId, String caption, Instant publishedAt) {

    static PostView of(Post post) {
        return new PostView(post.postId(), post.author(), post.mediaId(), post.caption(), post.publishedAt());
    }
}
