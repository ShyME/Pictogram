package me.imshy.pictogram.post.internal;

import java.util.*;
import java.util.stream.Collectors;
import me.imshy.pictogram.post.PublishedPost;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
class AuthoredPosts implements PublishedPosts {

    private final Posts posts;

    AuthoredPosts(Posts posts) {
        this.posts = posts;
    }

    @Override
    public Page byAuthors(Collection<UserId> authors, Cursor after, int limit) {
        if (authors.isEmpty() || limit < 1) {
            return new Page(List.of(), null);
        }

        Set<UUID> authorIds = authors.stream().map(UserId::value).collect(Collectors.toSet());
        Limit fetch = Limit.of(limit + 1);
        List<Post> rows = after == null
            ? posts.newestByAuthors(authorIds, fetch)
            : posts.byAuthorsBefore(authorIds, after.at(), after.id(), fetch);

        boolean hasMore = rows.size() > limit;
        List<Post> page = hasMore ? rows.subList(0, limit) : rows;

        Cursor nextCursor = null;
        if (hasMore) {
            Post last = page.get(page.size() - 1);
            nextCursor = new Cursor(last.publishedAt(), last.postId().value());
        }

        return new Page(page.stream().map(AuthoredPosts::toPublished).toList(), nextCursor);
    }

    @Override
    public Optional<UserId> authorOf(PostId post) {
        return posts.findById(post.value()).map(Post::author);
    }

    private static PublishedPost toPublished(Post post) {
        return new PublishedPost(post.postId(), post.author(), post.mediaId(), post.caption(), post.publishedAt());
    }
}
