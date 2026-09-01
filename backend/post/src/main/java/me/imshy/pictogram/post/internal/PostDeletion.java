package me.imshy.pictogram.post.internal;

import java.time.Clock;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.ForbiddenException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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
