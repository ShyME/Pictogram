package me.imshy.pictogram.post.internal;

import java.time.Clock;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.ForbiddenException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * The delete command (post/CONTEXT.md): an author permanently removes one of their own
 * posts. Hard delete — the row is gone, there is no trash and no undo. Only the author may
 * delete their post; anyone else is a {@link ForbiddenException} and the post is untouched.
 * A successful delete emits {@link PostDeleted}.
 *
 * <p>Not {@code @Transactional}, mirroring {@link Publishing}: the delete runs in its own
 * transaction and the event publishes after it, so there is no outer boundary for a listener
 * to roll back.
 */
@Service
public class PostDeletion {

    private final Posts posts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PostDeletion(Posts posts, ApplicationEventPublisher events, Clock clock) {
        this.posts = posts;
        this.events = events;
        this.clock = clock;
    }

    public void delete(UserId currentUser, PostId postId) {
        Post post = posts.findById(postId.value()).orElseThrow(PostNotFoundException::new);
        if (!post.author().equals(currentUser)) {
            throw new ForbiddenException("Only the author of a post can delete it.");
        }

        posts.delete(post);
        events.publishEvent(new PostDeleted(post.postId(), post.author(), post.mediaId(), clock.instant()));
    }
}
