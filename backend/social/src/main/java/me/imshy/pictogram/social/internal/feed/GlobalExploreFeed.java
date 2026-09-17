package me.imshy.pictogram.social.internal.feed;

import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.stereotype.Service;

@Service
class GlobalExploreFeed implements ExploreQuery {

    private final PublishedPosts posts;

    GlobalExploreFeed(PublishedPosts posts) {
        this.posts = posts;
    }

    @Override
    public Page pageFor(Cursor after, Integer limit) {
        PublishedPosts.Page page = posts.page(after, FeedPageSize.clamp(limit));
        return new Page(page.posts().stream().map(FeedPost::of).toList(), page.nextCursor());
    }
}
