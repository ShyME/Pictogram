package me.imshy.pictogram.social.internal.comment;

import java.time.Clock;
import java.util.UUID;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ForbiddenException;
import me.imshy.pictogram.social.CommentDeleted;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CommentRemoval {

    private final Comments comments;
    private final PublishedPosts posts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CommentRemoval(Comments comments, PublishedPosts posts, ApplicationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.posts = posts;
        this.events = events;
        this.clock = clock;
    }

    public void delete(ViewerId viewer, UUID commentId) {
        PostComment comment = comments.findById(commentId).map(Comment::view).orElse(null);
        if (comment == null) {
            return;
        }

        if (!mayDelete(viewer, comment)) {
            throw new ForbiddenException("Only a comment's author or the post's author can delete it.");
        }

        comments.deleteById(commentId);
        events.publishEvent(new CommentDeleted(comment.postId(), commentId, viewer, clock.instant()));
    }

    private boolean mayDelete(ViewerId viewer, PostComment comment) {
        if (comment.viewer().value().equals(viewer.value())) {
            return true;
        }
        return posts.authorOf(comment.postId())
                .map(author -> author.value().equals(viewer.value()))
                .orElse(false);
    }
}
