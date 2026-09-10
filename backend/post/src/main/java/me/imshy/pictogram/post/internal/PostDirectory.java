package me.imshy.pictogram.post.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import me.imshy.pictogram.shared.PostId;
import org.springframework.stereotype.Service;

/**
 * Batch lookup of posts by id, for callers that already hold a set of post ids
 * and want their media / caption in one call (the notifications screen, #199).
 * The read-model analogue of {@code ProfileDirectory.byIds}; public only so the
 * sibling {@code internal.web} controller can use it.
 */
@Service
public class PostDirectory {

    private final Posts posts;

    PostDirectory(Posts posts) {
        this.posts = posts;
    }

    public List<PostView> byIds(Collection<PostId> ids) {
        List<UUID> keys = ids.stream().map(PostId::value).toList();
        return StreamSupport.stream(posts.findAllById(keys).spliterator(), false).map(PostView::of).toList();
    }
}
