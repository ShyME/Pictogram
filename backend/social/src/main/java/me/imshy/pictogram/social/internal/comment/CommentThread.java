package me.imshy.pictogram.social.internal.comment;

import java.util.List;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.http.Cursor;
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
        int pageSize = clamp(limit);

        Limit fetch = Limit.of(pageSize + 1);
        List<Comment> rows = after == null
                ? comments.oldestFor(post.value(), fetch)
                : comments.afterFor(post.value(), after.at(), after.id(), fetch);

        boolean hasMore = rows.size() > pageSize;
        List<Comment> page = hasMore ? rows.subList(0, pageSize) : rows;

        Cursor nextCursor = null;
        if (hasMore) {
            Comment last = page.get(page.size() - 1);
            nextCursor = new Cursor(last.createdAt(), last.getId());
        }

        return new Page(page.stream().map(Comment::view).toList(), nextCursor);
    }

    private static int clamp(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
    }

    public record Page(List<PostComment> comments, Cursor nextCursor) {}
}
