package me.imshy.pictogram.social.internal.comment;

import java.util.List;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.KeysetWindow;
import me.imshy.pictogram.shared.http.Limits;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class CommentThread {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final Comments comments;

    CommentThread(Comments comments) {
        this.comments = comments;
    }

    public Page pageFor(PostId post, Cursor after, Integer limit) {
        int pageSize = Limits.clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        Limit fetch = Limit.of(pageSize + 1);
        List<Comment> rows = after == null
                ? comments.oldestFor(post.value(), fetch)
                : comments.afterFor(post.value(), after.at(), after.id(), fetch);

        KeysetWindow<Comment> window =
                KeysetWindow.of(rows, pageSize, last -> new Cursor(last.createdAt(), last.getId()));

        return new Page(window.page().stream().map(Comment::view).toList(), window.nextCursor());
    }

    public record Page(List<PostComment> comments, Cursor nextCursor) {}
}
