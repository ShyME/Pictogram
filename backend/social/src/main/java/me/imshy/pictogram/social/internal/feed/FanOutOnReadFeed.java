package me.imshy.pictogram.social.internal.feed;

import java.util.List;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.FollowGraph;
import org.springframework.stereotype.Service;

@Service
class FanOutOnReadFeed implements FeedQuery {

    static final int DEFAULT_LIMIT = 12;
    static final int MAX_LIMIT = 30;

    private final FollowGraph followGraph;
    private final PublishedPosts posts;

    FanOutOnReadFeed(FollowGraph followGraph, PublishedPosts posts) {
        this.followGraph = followGraph;
        this.posts = posts;
    }

    @Override
    public Page pageFor(ViewerId viewer, Cursor after, Integer limit) {
        List<UserId> followed = followGraph.usersFollowedBy(viewer);
        if (followed.isEmpty()) {
            return new Page(List.of(), null);
        }

        PublishedPosts.Page page = posts.byAuthors(followed, after, clamp(limit));
        return new Page(page.posts().stream().map(FeedPost::of).toList(), page.nextCursor());
    }

    private static int clamp(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
    }
}
