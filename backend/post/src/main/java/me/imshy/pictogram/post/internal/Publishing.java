package me.imshy.pictogram.post.internal;

import java.time.Clock;
import java.util.Optional;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.post.PostPublished;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * The publish command (post/CONTEXT.md): an author turns an already-uploaded image plus an
 * optional caption into a {@link Post}. The image must be media the author owns — checked
 * against {@link MediaCatalog}, media's published interface (CONTEXT-MAP: post → media) —
 * or the publish is rejected as {@link UnusableMediaException}. A successful publish emits
 * {@link PostPublished}.
 *
 * <p>Not {@code @Transactional}: the {@code save} runs in its own transaction and the event
 * publishes after it, so there is no outer boundary for a listener to roll back.
 */
@Service
public class Publishing {

    private final Posts posts;
    private final MediaCatalog media;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Publishing(Posts posts, MediaCatalog media, ApplicationEventPublisher events, Clock clock) {
        this.posts = posts;
        this.media = media;
        this.events = events;
        this.clock = clock;
    }

    public PostView publish(UserId author, MediaId mediaId, String caption) {
        Caption text = Caption.of(caption);

        Optional<UserId> owner = media.ownerOf(mediaId);
        if (owner.isEmpty() || !owner.get().equals(author)) {
            throw new UnusableMediaException();
        }

        Post post = Post.publish(author, mediaId, text, clock.instant());
        posts.save(post);
        events.publishEvent(new PostPublished(post.postId(), author, mediaId, post.publishedAt()));
        return PostView.of(post);
    }
}
