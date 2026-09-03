package me.imshy.pictogram.social.internal.feed;

import java.time.Instant;
import me.imshy.pictogram.post.PublishedPost;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

public record FeedPost(PostId postId, UserId authorId, MediaId mediaId, String caption, Instant publishedAt) {

    static FeedPost of(PublishedPost post) {
        return new FeedPost(post.postId(), post.author(), post.mediaId(), post.caption(), post.publishedAt());
    }
}
