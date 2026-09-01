package me.imshy.pictogram.post.internal;

import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * The read side of {@code post}: one author's posts, newest first, keyset-paged so posts
 * published between page fetches cause neither duplicates nor skips. The client composes the
 * profile grid from this (ADR-0005).
 */
@Service
public class PostTimeline {

    static final int DEFAULT_LIMIT = 24;
    static final int MAX_LIMIT = 48;

    private final Posts posts;

    PostTimeline(Posts posts) {
        this.posts = posts;
    }

    /**
     * @param after {@code null} for the first page, otherwise the position the previous
     *              page's {@code nextCursor} decoded to
     * @param limit requested page size; clamped to {@code [1, MAX_LIMIT]}, defaulting when null
     */
    public Page pageFor(UserId author, Cursor after, Integer limit) {
        int pageSize = clamp(limit);

        // Fetch one extra row to learn whether a further page exists without a count query.
        Limit fetch = Limit.of(pageSize + 1);
        List<Post> rows = after == null
                ? posts.newestBy(author.value(), fetch)
                : posts.pageBy(author.value(), after.publishedAt(), after.id(), fetch);

        boolean hasMore = rows.size() > pageSize;
        List<Post> page = hasMore ? rows.subList(0, pageSize) : rows;

        Cursor nextCursor = null;
        if (hasMore) {
            Post last = page.get(page.size() - 1);
            nextCursor = new Cursor(last.publishedAt(), last.postId().value());
        }

        return new Page(page.stream().map(PostView::of).toList(), nextCursor);
    }

    private static int clamp(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.clamp(limit, 1, MAX_LIMIT);
    }

    /** One page of the timeline, mirroring the {@code ApiPage} envelope's {@code items} / cursor. */
    public record Page(List<PostView> items, Cursor nextCursor) {
    }
}
