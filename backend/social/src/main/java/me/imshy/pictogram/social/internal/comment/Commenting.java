package me.imshy.pictogram.social.internal.comment;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostCommented;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class Commenting {

    private final Comments comments;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Commenting(Comments comments, ApplicationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.events = events;
        this.clock = clock;
    }

    public PostComment comment(ViewerId viewer, PostId post, String body) {
        Instant createdAt = clock.instant();
        PostComment saved = comments.save(Comment.write(viewer, post, CommentBody.of(body), createdAt))
                .view();
        events.publishEvent(new PostCommented(post, saved.commentId(), viewer, createdAt));
        return saved;
    }
}
