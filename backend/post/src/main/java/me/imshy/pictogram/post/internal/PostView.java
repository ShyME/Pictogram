package me.imshy.pictogram.post.internal;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

public record PostView(PostId postId, UserId authorId, MediaId mediaId, String caption, Instant publishedAt) {

    static PostView of(Post post) {
        return new PostView(post.postId(), post.author(), post.mediaId(), post.caption(), post.publishedAt());
    }
}
