package me.imshy.pictogram.post.internal;

import java.time.Clock;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.post.PostPublished;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PostPublishing {

    private final Posts posts;
    private final MediaCatalog media;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PostPublishing(Posts posts, MediaCatalog media, ApplicationEventPublisher events, Clock clock) {
        this.posts = posts;
        this.media = media;
        this.events = events;
        this.clock = clock;
    }

    public PostView publish(UserId author, MediaId mediaId, String caption) {
        Caption text = Caption.of(caption);

        if (media.ownerOf(mediaId).filter(author::equals).isEmpty()) {
            throw new UnusableMediaException();
        }

        Post post = Post.publish(author, mediaId, text, clock.instant());
        posts.save(post);
        events.publishEvent(new PostPublished(post.postId(), author, mediaId, post.publishedAt()));
        return PostView.of(post);
    }
}
