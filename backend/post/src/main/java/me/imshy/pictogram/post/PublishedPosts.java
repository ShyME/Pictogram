package me.imshy.pictogram.post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;

public interface PublishedPosts {

    Page byAuthors(Collection<UserId> authors, Cursor after, int limit);

    /** Who published a post, or empty if no post has that id. */
    Optional<UserId> authorOf(PostId post);

    record Page(List<PublishedPost> posts, Cursor nextCursor) {}
}
