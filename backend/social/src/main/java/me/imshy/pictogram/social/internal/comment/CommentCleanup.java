package me.imshy.pictogram.social.internal.comment;

import me.imshy.pictogram.post.PostDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Clears a post's thread when the post is deleted, so no comment row outlives its post.
 * Synchronous, in the deleting transaction — v1 has no event registry (ADR-0002).
 * {@code CommentCleanupTest} pins the behaviour.
 */
@Component
class CommentCleanup {

    private final Comments comments;

    CommentCleanup(Comments comments) {
        this.comments = comments;
    }

    @EventListener
    void onPostDeleted(PostDeleted event) {
        comments.deleteByPostId(event.postId().value());
    }
}
