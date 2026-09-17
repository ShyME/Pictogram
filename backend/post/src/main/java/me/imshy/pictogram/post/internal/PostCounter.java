package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.post.PostStats;
import org.springframework.stereotype.Service;

// Its own bean, not folded into AuthoredPosts: PublishedPosts is a widely-mocked seam
// (@MockitoBean in other modules' and app's tests) — combining PostStats onto the same
// bean would make a PublishedPosts mock silently stop satisfying PostStats too.
@Service
class PostCounter implements PostStats {

    private final Posts posts;

    PostCounter(Posts posts) {
        this.posts = posts;
    }

    @Override
    public long total() {
        return posts.count();
    }
}
