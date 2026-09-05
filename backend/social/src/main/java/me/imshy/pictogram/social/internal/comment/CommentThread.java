package me.imshy.pictogram.social.internal.comment;

import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.ForbiddenException;
import me.imshy.pictogram.shared.http.KeysetWindow;
import me.imshy.pictogram.shared.http.Limits;
import me.imshy.pictogram.social.CommentCounts;
import me.imshy.pictogram.social.CommentDeleted;
import me.imshy.pictogram.social.PostCommented;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class CommentThread implements CommentCounts {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final Comments comments;
    private final PublishedPosts publishedPosts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CommentThread(Comments comments, PublishedPosts publishedPosts, ApplicationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.publishedPosts = publishedPosts;
        this.events = events;
        this.clock = clock;
    }

    public PostComment comment(ViewerId viewer, PostId post, String body) {
        Instant createdAt = clock.instant();
        PostComment saved = comments.save(Comment.write(viewer, post, CommentBody.of(body), createdAt)).view();
        events.publishEvent(new PostCommented(post, saved.commentId(), viewer, createdAt));
        return saved;
    }

    public Page pageFor(PostId post, Cursor after, Integer limit) {
        int pageSize = Limits.clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        Limit fetch = Limit.of(pageSize + 1);
        List<Comment> rows = after == null
            ? comments.oldestFor(post.value(), fetch)
            : comments.afterFor(post.value(), after.at(), after.id(), fetch);

        KeysetWindow<Comment> window = KeysetWindow.of(rows, pageSize,
            last -> new Cursor(last.createdAt(), last.getId()));

        return new Page(window.page().stream().map(Comment::view).toList(), window.nextCursor());
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
        return publishedPosts.authorOf(comment.postId()).map(author -> author.value().equals(viewer.value()))
            .orElse(false);
    }

    @Override
    public List<PostComments> of(Collection<PostId> posts) {
        List<UUID> ids = posts.stream().map(PostId::value).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = comments.countsFor(ids).stream()
            .collect(toMap(CommentCount::postId, CommentCount::count));
        return ids.stream().map(id -> new PostComments(new PostId(id), counts.getOrDefault(id, 0L))).toList();
    }

    @EventListener
    void onPostDeleted(PostDeleted event) {
        comments.deleteByPostId(event.postId().value());
    }

    public record Page(List<PostComment> comments, Cursor nextCursor) {
    }
}
